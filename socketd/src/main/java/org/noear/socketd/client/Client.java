package org.noear.socketd.client;

import org.noear.socketd.protocol.HeartbeatHandler;
import org.noear.socketd.protocol.Listener;
import org.noear.socketd.protocol.Session;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 客户端
 *
 * @author noear
 * @since 2.0
 */
public interface Client {
    /**
     * 连接地址
     */
    Client url(String url);
    /**
     * 自动重链
     * */
    Client autoReconnect(boolean enable);

    Client sslContext(SSLContext sslContext);
    /**
     * 心跳
     * */
    Client heartbeatHandler(HeartbeatHandler handler);
    /**
     * 监听
     */
    Client listen(Listener listener);
    /**
     * 打开
     */
    Session open() throws IOException, TimeoutException;
}
