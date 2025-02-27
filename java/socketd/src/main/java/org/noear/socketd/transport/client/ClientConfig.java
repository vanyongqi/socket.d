package org.noear.socketd.transport.client;

import org.noear.socketd.transport.core.internal.ConfigBase;

import java.net.URI;

/**
 * 客记端配置（单位：毫秒）
 *
 * @author noear
 * @since 2.0
 */
public class ClientConfig extends ConfigBase<ClientConfig> {
    //通讯架构（tcp, ws, udp）
    private final String schema;

    //连接地址
    private final String linkUrl;
    private final String url;
    private final URI uri;
    private int port;

    //心跳间隔（毫秒）
    private long heartbeatInterval;

    //连接越时（毫秒）
    private long connectTimeout;

    //是否自动重链
    private boolean autoReconnect;


    public ClientConfig(String url) {
        super(true);

        //支持 sd: 开头的架构
        if (url.startsWith("sd:")) {
            url = url.substring(3);
        }

        this.url = url;
        this.uri = URI.create(url);
        this.port = uri.getPort();
        this.schema = uri.getScheme();
        this.linkUrl = "sd:" + url;

        if (this.port < 0) {
            this.port = 8602;
        }

        this.connectTimeout = 10_000;
        this.heartbeatInterval = 20_000;

        this.autoReconnect = true;
    }


    /**
     * 获取通讯架构（tcp, ws, udp）
     */
    public String getSchema() {
        return schema;
    }


    /**
     * 获取连接地址
     */
    public String getUrl() {
        return url;
    }

    /**
     * 获取连接地址
     */
    public URI getUri() {
        return uri;
    }

    /**
     * 获取链接地址
     */
    public String getLinkUrl() {
        return linkUrl;
    }

    /**
     * 获取连接主机
     */
    public String getHost() {
        return uri.getHost();
    }

    /**
     * 获取连接端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取心跳间隔（单位毫秒）
     */
    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * 配置心跳间隔（单位毫秒）
     */
    public ClientConfig heartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
        return this;
    }

    /**
     * 获取连接超时（单位毫秒）
     */
    public long getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 配置连接超时（单位毫秒）
     */
    public ClientConfig connectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 获取是否自动重链
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    /**
     * 配置是否自动重链
     */
    public ClientConfig autoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }

    @Override
    public ClientConfig idleTimeout(int idleTimeout) {
        if (autoReconnect == false) {
            //自动重链下，禁用 idleTimeout
            return super.idleTimeout(idleTimeout);
        } else {
            return super.idleTimeout(0);
        }
    }

    @Override
    public String toString() {
        return "ClientConfig{" +
                "schema='" + schema + '\'' +
                ", charset=" + charset +
                ", url='" + url + '\'' +
                ", heartbeatInterval=" + heartbeatInterval +
                ", connectTimeout=" + connectTimeout +
                ", idleTimeout=" + idleTimeout +
                ", requestTimeout=" + requestTimeout +
                ", readBufferSize=" + readBufferSize +
                ", writeBufferSize=" + writeBufferSize +
                ", autoReconnect=" + autoReconnect +
                ", maxUdpSize=" + maxUdpSize +
                '}';
    }
}