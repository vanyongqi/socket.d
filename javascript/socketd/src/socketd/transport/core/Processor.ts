import {Listener, SimpleListener} from "./Listener";
import type {ChannelInternal} from "./Channel";
import type {Message} from "./Message";
import type {Frame} from "./Frame";
import {Constants, EntityMetas, Flags} from "./Constants";
import {SocketdAlarmException, SocketdConnectionException} from "../../exception/SocketdException";
import {HandshakeDefault} from "./HandshakeDefault";

/**
 * 处理器
 *
 * @author noear
 * @since 2.0
 */
export interface Processor {
    /**
     * 设置监听器
     */
    setListener(listener: Listener);

    /**
     * 接收处理
     */
    onReceive(channel: ChannelInternal, frame: Frame);

    /**
     * 打开时
     *
     * @param channel 通道
     */
    onOpen(channel: ChannelInternal);

    /**
     * 收到消息时
     *
     * @param channel 通道
     * @param message 消息
     */
    onMessage(channel: ChannelInternal, message: Message);


    /**
     * 关闭时
     *
     * @param channel 通道
     */
    onClose(channel: ChannelInternal);


    /**
     * 出错时
     *
     * @param channel 通道
     * @param error   错误信息
     */
    onError(channel: ChannelInternal, error: Error);
}

export class ProcessorDefault implements Processor {
    private _listener: Listener;

    constructor() {
        this._listener = new SimpleListener();
    }

    setListener(listener: Listener) {
        if (listener != null) {
            this._listener = listener;
        }
    }


    onReceive(channel: ChannelInternal, frame:Frame) {
        // if (channel.getConfig().clientMode()) {
        //     console.trace("C-REV:" + frame);
        // } else {
        //     console.trace("S-REV:" + frame);
        // }

        if (frame.flag() == Flags.Connect) {
            channel.setHandshake(new HandshakeDefault(frame.message()!));
            channel.onOpenFuture((r, err) => {
                if (r && channel.isValid()) {
                    //如果还有效，则发送链接确认
                    try {
                        channel.sendConnack(frame.message()!); //->Connack
                    } catch (err) {
                        this.onError(channel, err);
                    }
                }
            })
            this.onOpen(channel);
        } else if (frame.flag() == Flags.Connack) {
            //if client
            channel.setHandshake(new HandshakeDefault(frame.message()!));
            this.onOpen(channel);
        } else {
            if (channel.getHandshake() == null) {
                channel.close(Constants.CLOSE1_PROTOCOL);

                if (frame.flag() == Flags.Close) {
                    //说明握手失败了
                    throw new SocketdConnectionException("Connection request was rejected");
                }

                console.warn(`${channel.getConfig().getRoleName()} channel handshake is null, sessionId=${channel.getSession().sessionId()}`);
                return
            }

            try {
                switch (frame.flag()) {
                    case Flags.Ping: {
                        channel.sendPong();
                        break;
                    }
                    case Flags.Pong: {
                        break;
                    }
                    case Flags.Close: {
                        //关闭通道
                        channel.close(Constants.CLOSE1_PROTOCOL);
                        this.onCloseInternal(channel);
                        break;
                    }
                    case Flags.Alarm: {
                        //结束流，并异常通知
                        const exception = new SocketdAlarmException(frame.message()!);
                        const stream = channel.getConfig().getStreamManger().getStream(frame.message()!.sid());
                        if (stream == null) {
                            this.onError(channel, exception);
                        } else {
                            channel.getConfig().getStreamManger().removeStream(frame.message()!.sid());
                            stream.onError(exception);
                        }
                        break;
                    }
                    case Flags.Message:
                    case Flags.Request:
                    case Flags.Subscribe: {
                        this.onReceiveDo(channel, frame, false);
                        break;
                    }
                    case Flags.Reply:
                    case Flags.ReplyEnd: {
                        this.onReceiveDo(channel, frame, true);
                        break;
                    }
                    default: {
                        channel.close(Constants.CLOSE2_PROTOCOL_ILLEGAL);
                        this.onCloseInternal(channel);
                    }
                }
            } catch (e) {
                this.onError(channel, e);
            }
        }
    }

    onReceiveDo(channel: ChannelInternal, frame:Frame, isReply) {
        //如果启用了聚合!
        if(channel.getConfig().getFragmentHandler().aggrEnable()) {
            //尝试聚合分片处理
            const fragmentIdxStr = frame.message()!.meta(EntityMetas.META_DATA_FRAGMENT_IDX);
            if (fragmentIdxStr != null) {
                //解析分片索引
                const index = parseInt(fragmentIdxStr);
                const frameNew = channel.getConfig().getFragmentHandler().aggrFragment(channel, index, frame.message()!);

                if (frameNew == null) {
                    return;
                } else {
                    frame = frameNew;
                }
            }
        }

        //执行接收处理
        if (isReply) {
            channel.retrieve(frame);
        } else {
            this.onMessage(channel, frame.message());
        }
    }

    onOpen(channel: ChannelInternal) {
        try {
            this._listener.onOpen(channel.getSession())
            channel.doOpenFuture(true, null);
        } catch (e) {
            console.warn(`${channel.getConfig().getRoleName()} channel listener onOpen error`, e);

            channel.doOpenFuture(false, e);
        }
    }

    onMessage(channel: ChannelInternal, message) {
        try {
            this._listener.onMessage(channel.getSession(), message)
        } catch (e) {
            console.warn(`${channel.getConfig().getRoleName()} channel listener onMessage error`, e);

            this.onError(channel, e);
        }
    }

    onClose(channel: ChannelInternal) {
        if (channel.isClosed() == 0) {
            this.onCloseInternal(channel);
        }
    }

    onCloseInternal(channel: ChannelInternal) {
        this._listener.onClose(channel.getSession())
    }

    onError(channel: ChannelInternal, error: any) {
        this._listener.onError(channel.getSession(), error)
    }
}
