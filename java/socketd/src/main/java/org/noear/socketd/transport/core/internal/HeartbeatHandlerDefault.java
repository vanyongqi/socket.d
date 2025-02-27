package org.noear.socketd.transport.core.internal;

import org.noear.socketd.transport.core.HeartbeatHandler;
import org.noear.socketd.transport.core.Session;

/**
 * 心跳处理默认实现
 *
 * @author noear
 * @since 2.0
 */
public class HeartbeatHandlerDefault implements HeartbeatHandler {
    /**
     * 心跳处理
     */
    @Override
    public void heartbeat(Session session) throws Exception {
        session.sendPing();
    }
}
