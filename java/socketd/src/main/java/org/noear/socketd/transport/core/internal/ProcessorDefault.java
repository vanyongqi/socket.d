package org.noear.socketd.transport.core.internal;

import org.noear.socketd.exception.SocketdAlarmException;
import org.noear.socketd.exception.SocketdConnectionException;
import org.noear.socketd.transport.core.*;
import org.noear.socketd.transport.core.listener.SimpleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 处理器默认实现
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
    public void onReceive(Channel channel, Frame frame) throws IOException {
        if (log.isDebugEnabled()) {
            if (channel.getConfig().clientMode()) {
                log.debug("C-REV:{}", frame);
            } else {
                log.debug("S-REV:{}", frame);
            }
        }

        if (frame.getFlag() == Flags.Connect) {
            //if server
            HandshakeDefault handshake = new HandshakeDefault(frame.getMessage());
            channel.setHandshake(handshake);

            //开始打开（可用于 url 签权）//禁止发消息
            onOpen(channel);

            if (channel.isValid()) {
                //如果还有效，则发送链接确认
                channel.sendConnack(frame.getMessage()); //->Connack
            }
        } else if (frame.getFlag() == Flags.Connack) {
            //if client
            HandshakeDefault handshake = new HandshakeDefault(frame.getMessage());
            channel.setHandshake(handshake);

            onOpen(channel);
        } else {
            if (channel.getHandshake() == null) {
                channel.close(Constants.CLOSE1_PROTOCOL);

                if(frame.getFlag() == Flags.Close){
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
                switch (frame.getFlag()) {
                    case Flags.Ping: {
                        channel.sendPong();
                        break;
                    }
                    case Flags.Pong: {
                        break;
                    }
                    case Flags.Close: {
                        channel.close(Constants.CLOSE1_PROTOCOL);
                        onCloseInternal(channel);
                        break;
                    }
                    case Flags.Alarm:{
                        onError(channel, new SocketdAlarmException(frame.getMessage()));
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
                        channel.close(Constants.CLOSE1_PROTOCOL);
                        onClose(channel);
                    }
                }
            } catch (Throwable e) {
                onError(channel, e);
            }
        }
    }

    private void onReceiveDo(Channel channel, Frame frame, boolean isReply) throws IOException {
        //尝试分片处理
        String fragmentIdxStr = frame.getMessage().meta(EntityMetas.META_DATA_FRAGMENT_IDX);
        if (fragmentIdxStr != null) {
            //解析分片索引
            int index = Integer.parseInt(fragmentIdxStr);
            Frame frameNew = channel.getConfig().getFragmentHandler().aggrFragment(channel, index, frame.getMessage());

            if (frameNew == null) {
                return;
            } else {
                frame = frameNew;
            }
        }

        //执行接收处理
        if (isReply) {
            channel.retrieve(frame, error -> {
                onError(channel, error);
            });
        } else {
            onMessage(channel, frame.getMessage());
        }
    }


    /**
     * 打开时
     *
     * @param channel 通道
     */
    @Override
    public void onOpen(Channel channel) throws IOException {
        listener.onOpen(channel.getSession());
    }

    /**
     * 收到消息时
     *
     * @param channel 通道
     * @param message 消息
     */
    @Override
    public void onMessage(Channel channel, Message message) throws IOException {
        channel.getConfig().getChannelExecutor().submit(() -> {
            try {
                listener.onMessage(channel.getSession(), message);
            } catch (Throwable e) {
                if (log.isWarnEnabled()) {
                    log.warn("{} channel listener onMessage error",
                            channel.getConfig().getRoleName(), e);
                }
            }
        });
    }

    /**
     * 关闭时
     *
     * @param channel 通道
     */
    @Override
    public void onClose(Channel channel) {
        if (channel.isClosed() == 0) {
            onCloseInternal(channel);
        }
    }

    /**
     * 关闭时（内部处理）
     *
     * @param channel 通道
     */
    private void onCloseInternal(Channel channel){
        listener.onClose(channel.getSession());
    }

    /**
     * 出错时
     *
     * @param channel 通道
     * @param error   错误信息
     */
    @Override
    public void onError(Channel channel, Throwable error) {
        listener.onError(channel.getSession(), error);
    }
}