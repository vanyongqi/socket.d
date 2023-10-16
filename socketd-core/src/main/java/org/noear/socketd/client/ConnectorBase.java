package org.noear.socketd.client;

import org.noear.socketd.protocol.HeartbeatHandler;
import org.noear.socketd.protocol.Listener;

import javax.net.ssl.SSLContext;
import java.net.URI;

/**
 * @author noear
 * @since 2.0
 */
public abstract class ConnectorBase implements Connector {
    protected String url;
    protected URI uri;
    protected boolean autoReconnect;
    protected Listener listener;
    protected SSLContext sslContext;
    protected HeartbeatHandler heartbeatHandler;

    public ConnectorBase(String uri) {
        this.url = url;
        this.uri = URI.create(uri);
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public boolean autoReconnect() {
        return autoReconnect;
    }

    @Override
    public Connector autoReconnect(boolean enable) {
        this.autoReconnect = enable;
        return this;
    }

    @Override
    public Connector ssl(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    @Override
    public Connector heartbeat(HeartbeatHandler handler) {
        this.heartbeatHandler = handler;
        return this;
    }

    @Override
    public Connector listen(Listener listener) {
        this.listener = listener;
        return this;
    }
}
