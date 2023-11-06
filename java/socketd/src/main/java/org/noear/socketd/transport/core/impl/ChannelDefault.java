package org.noear.socketd.transport.core.impl;

import org.noear.socketd.transport.core.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通道默认实现（每个连接都会建立一个通道）
 *
 * @author noear
 * @since 2.0
 */
public class ChannelDefault<S> extends ChannelBase implements Channel {
    private final S source;

    //接收管理器
    private final Map<String, Acceptor> acceptorMap;
    //助理
    private final ChannelAssistant<S> assistant;
    //会话（懒加载）
    private Session session;

    public ChannelDefault(S source, Config config, ChannelAssistant<S> assistant) {
        super(config);
        this.source = source;
        this.assistant = assistant;
        this.acceptorMap = new ConcurrentHashMap<>();
    }

    @Override
    public void removeAcceptor(String sid) {
        acceptorMap.remove(sid);
    }

    @Override
    public boolean isValid() {
        return assistant.isValid(source);
    }

    @Override
    public InetSocketAddress getRemoteAddress() throws IOException {
        return assistant.getRemoteAddress(source);
    }

    @Override
    public InetSocketAddress getLocalAddress() throws IOException {
        return assistant.getLocalAddress(source);
    }

    /**
     * 发送
     */
    @Override
    public synchronized void send(Frame frame, Acceptor acceptor) throws IOException {
        Asserts.assertClosed(this);

        if (frame.getMessage() != null) {
            Message message = frame.getMessage();

            //注册接收器
            if (acceptor != null) {
                acceptorMap.put(message.getSid(), acceptor);
            }

            //尝试分片
            if (message.getEntity() != null) {
                //确保用完自动关闭
                try (InputStream ins = message.getEntity().getData()) {
                    if (message.getEntity().getDataSize() > Config.MAX_SIZE_FRAGMENT) {
                        AtomicReference<Integer> fragmentIndex = new AtomicReference<>(0);
                        while (true) {
                            Entity fragmentEntity = getConfig().getFragmentHandler().nextFragment(getConfig(), fragmentIndex, message.getEntity());

                            if (fragmentEntity != null) {
                                //主要是 sid 和 entity
                                Frame fragmentFrame = new Frame(frame.getFlag(), new MessageDefault()
                                        .flag(frame.getFlag())
                                        .sid(message.getSid())
                                        .entity(fragmentEntity));

                                assistant.write(source, fragmentFrame);
                            } else {
                                return;
                            }
                        }
                    } else {
                        assistant.write(source, frame);
                        return;
                    }
                }
            }
        }

        assistant.write(source, frame);
    }

    /**
     * 收回（收回答复）
     */
    @Override
    public void retrieve(Frame frame) throws IOException {
        Acceptor acceptor = acceptorMap.get(frame.getMessage().getSid());

        if (acceptor != null) {
            if (acceptor.isSingle() || frame.getFlag() == Flag.ReplyEnd) {
                acceptorMap.remove(frame.getMessage().getSid());
            }

            acceptor.accept(frame.getMessage());
        }
    }

    /**
     * 获取会话
     */
    @Override
    public Session getSession() {
        if (session == null) {
            session = new SessionDefault(this);
        }

        return session;
    }

    /**
     * 关闭
     */
    @Override
    public void close() throws IOException {
        super.close();
        acceptorMap.clear();
        assistant.close(source);
    }
}