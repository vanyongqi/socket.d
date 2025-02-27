package org.noear.socketd.transport.core;

import java.util.function.BiConsumer;

/**
 * 通道内部接口
 *
 * @author noear
 * @since 2.0
 */
public interface ChannelInternal extends Channel {
    /**
     * 设置会话
     */
    void setSession(Session session);

    /**
     * 当打开时
     */
    void onOpenFuture(BiConsumer<Boolean, Throwable> future);

    /**
     * 执行打开时
     */
    void doOpenFuture(boolean isOk, Throwable error);
}
