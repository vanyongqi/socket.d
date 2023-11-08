package org.noear.socketd.transport.core;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 握手信息
 *
 * @author noear
 * @since 2.0
 */
public class Handshake {
    private final URI uri;
    private final Entity entity;
    private final String version;
    private final Map<String, String> paramMap;

    public Handshake(Message message) {
        this.uri = URI.create(message.getTopic());
        this.entity = message.getEntity();
        this.version = entity.getMeta(EntityMetas.META_SOCKETD_VERSION);
        this.paramMap = new HashMap<>();

        String queryString = uri.getQuery();
        if (queryString != null) {
            for (String kvStr : queryString.split("&")) {
                String[] kv = kvStr.split("=");
                if (kv.length > 1) {
                    paramMap.put(kv[0], kv[1]);
                } else {
                    paramMap.put(kv[0], "");
                }
            }
        }
    }

    /**
     * 获请地址
     *
     * @return tcp://192.168.0.1/path?user=1&path=2
     */
    public URI getUri() {
        return uri;
    }

    /**
     * 获取传输协议
     */
    public String getScheme() {
        return uri.getScheme();
    }

    /**
     * 获取路径
     */
    public String getPath() {
        return uri.getPath();
    }

    /**
     * 获取参数集合
     */
    public Map<String, String> getParamMap() {
        return paramMap;
    }

    /**
     * 获取参数
     */
    public String getParam(String name) {
        return paramMap.get(name);
    }

    /**
     * 版本
     */
    public String getVersion() {
        return version;
    }
}
