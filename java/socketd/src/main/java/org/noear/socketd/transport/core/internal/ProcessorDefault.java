package org.noear.socketd.transport.core.internal;

import org.noear.socketd.exception.SocketdAlarmException;
import org.noear.socketd.exception.SocketdConnectionException;
import org.noear.socketd.transport.core.*;
import org.noear.socketd.transport.core.listener.SimpleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 协议处理器默认实现
 *
 * @author noear
 * @since 2.0
 */
public class ProcessorDefault implements Processor {
    private static Logger log = LoggerFactory.getLogger(ProcessorDefault.class);

    private Listener listener = new SimpleListener();

    /**
     * 设置监听
     */
    @Override
    public void setListener(Listener listener) {
        if (listener != null) {
            this.listener = listener;
        }
    }

    /**
     * 接收处理
     */
    public void onReceive(ChannelInternal channel, Frame frame)  {
        if (log.isDebugEnabled()) {
            if (channel.getConfig().clientMode()) {
                log.debug("C-REV:{}", frame);
            } else {
                log.debug("S-REV:{}", frame);
            }
        }

        if (frame.flag() == Flags.Connect) {
            //if server
            HandshakeDefault handshake = new HandshakeDefault(frame.message());
            channel.setHandshake(handshake);

            //开始打开（可用于 url 签权）//禁止发消息
            channel.onOpenFuture((r,e)-> {
                if (e == null) {
                    //如果无异常
                    if (channel.isValid()) {
                        //如果还有效，则发送链接确认
                        try {
                            channel.sendConnack(frame.message()); //->Connack
                        } catch (Throwable err) {
                            onError(channel, err);
                        }
                    }
                } else {
                    //如果有异常
                    if (channel.isValid()) {
                        //如果还有效，则关闭通道
                        channel.close(Constants.CLOSE3_ERROR);
                        onCloseInternal(channel);
                    }
                }
            });
            onOpen(channel);
        } else if (frame.flag() == Flags.Connack) {
            //if client
            HandshakeDefault handshake = new HandshakeDefault(frame.message());
            channel.setHandshake(handshake);

            onOpen(channel);
        } else {
            if (channel.getHandshake() == null) {
                channel.close(Constants.CLOSE1_PROTOCOL);

                if(frame.flag() == Flags.Close){
                    //说明握手失败了
                    throw new SocketdConnectionException("Connection request was rejected");
                }

                if (log.isWarnEnabled()) {
                    log.warn("{} channel handshake is null, sessionId={}",
                            channel.getConfig().getRoleName(),
                            channel.getSession().sessionId());
                }
                return;
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
                        onCloseInternal(channel);
                        break;
                    }
                    case Flags.Alarm: {
                        //结束流，并异常通知
                        SocketdAlarmException exception = new SocketdAlarmException(frame.message());
                        StreamInternal stream = channel.getConfig().getStreamManger().getStream(frame.message().sid());
                        if (stream == null) {
                            onError(channel, exception);
                        } else {
                            channel.getConfig().getStreamManger().removeStream(frame.message().sid());
                            stream.onError(exception);
                        }
                        break;
                    }
                    case Flags.Message:
                    case Flags.Request:
                    case Flags.Subscribe: {
                        onReceiveDo(channel, frame, false);
                        break;
                    }
                    case Flags.Reply:
                    case Flags.ReplyEnd: {
                        onReceiveDo(channel, frame, true);
                        break;
                    }
                    default: {
                        channel.close(Constants.CLOSE2_PROTOCOL_ILLEGAL);
                        onCloseInternal(channel);
                    }
                }
            } catch (Throwable e) {
                onError(channel, e);
            }
        }
    }

    private void onReceiveDo(ChannelInternal channel, Frame frame, boolean isReply) throws IOException {
        //如果启用了聚合!
        if(channel.getConfig().getFragmentHandler().aggrEnable()) {
            //尝试聚合分片处理
            String fragmentIdxStr = frame.message().meta(EntityMetas.META_DATA_FRAGMENT_IDX);
            if (fragmentIdxStr != null) {
                //解析分片索引
                int index = Integer.parseInt(fragmentIdxStr);
                Frame frameNew = channel.getConfig().getFragmentHandler().aggrFragment(channel, index, frame.message());

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
            onMessage(channel, frame.message());
        }
    }


    /**
     * 打开时
     *
     * @param channel 通道
     */
    @Override
    public void onOpen(ChannelInternal channel) {
        channel.getConfig().getChannelExecutor().submit(() -> {
            try {
                listener.onOpen(channel.getSession());
                channel.doOpenFuture(true, null);
            } catch (Throwable e) {
                if (log.isWarnEnabled()) {
                    log.warn("{} channel listener onOpen error",
                            channel.getConfig().getRoleName(), e);
                }
                channel.doOpenFuture(false, e);
            }
        });
    }

    /**
     * 收到消息时
     *
     * @param channel 通道
     * @param message 消息
     */
    @Override
    public void onMessage(ChannelInternal channel, Message message) {
        channel.getConfig().getChannelExecutor().submit(() -> {
            try {
                listener.onMessage(channel.getSession(), message);
            } catch (Throwable e) {
                if (log.isWarnEnabled()) {
                    log.warn("{} channel listener onMessage error",
                            channel.getConfig().getRoleName(), e);
                }
                onError(channel, e);
            }
        });
    }

    /**
     * 关闭时
     *
     * @param channel 通道
     */
    @Override
    public void onClose(ChannelInternal channel) {
        if (channel.isClosed() == 0) {
            onCloseInternal(channel);
        }
    }

    /**
     * 关闭时（内部处理）
     *
     * @param channel 通道
     */
    private void onCloseInternal(ChannelInternal channel){
        listener.onClose(channel.getSession());
    }

    /**
     * 出错时
     *
     * @param channel 通道
     * @param error   错误信息
     */
    @Override
    public void onError(ChannelInternal channel, Throwable error) {
        listener.onError(channel.getSession(), error);
    }
}