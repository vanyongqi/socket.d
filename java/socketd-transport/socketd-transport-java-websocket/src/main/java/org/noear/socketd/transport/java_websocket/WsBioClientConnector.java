package org.noear.socketd.transport.java_websocket;

import org.noear.socketd.exception.SocketdTimeoutException;
import org.noear.socketd.transport.java_websocket.impl.WebSocketClientImpl;
import org.noear.socketd.transport.client.ClientConnectorBase;
import org.noear.socketd.transport.core.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Ws-Bio 客户端连接器实现（支持 ssl）
 *
 * @author noear
 * @since 2.0
 */
public class WsBioClientConnector extends ClientConnectorBase<WsBioClient> {
    private static final Logger log = LoggerFactory.getLogger(WsBioClientConnector.class);

    private WebSocketClientImpl real;

    public WsBioClientConnector(WsBioClient client) {
        super(client);
    }

    @Override
    public Channel connect() throws Exception {
        log.debug("Start connecting to: {}", client.config().getUrl());

        //处理自定义架构的影响
        String wsUrl = client.config().getUrl().replace("-java://", "://");
        real = new WebSocketClientImpl(URI.create(wsUrl), client);

        //支持 ssl
        if (client.config().getSslContext() != null) {
            real.setSocketFactory(client.config().getSslContext().getSocketFactory());
        }

        //闲置超时
        if(client.config().getIdleTimeout() > 0L) {
            //单位：毫秒
            real.setConnectionLostTimeout((int) (client.config().getIdleTimeout() / 1000L));
        }


        real.connect();

        try {
            return real.getChannel().get(client.config().getConnectTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            close();
            throw new SocketdTimeoutException("Connection timeout: " + client.config().getUrl());
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (real == null) {
            return;
        }

        try {
            real.close();
        } catch (Throwable e) {
            log.debug("{}", e);
        }
    }
}
