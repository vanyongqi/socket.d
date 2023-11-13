package org.noear.socketd.transport.core;

import java.net.URI;
import java.util.Map;

/**
 * 握手信息
 *
 * @author noear
 * @since 2.0
 */
public interface Handshake {
    /**
     * 协议版本
     */
    String version();

    /**
     * 获请传输地址
     *
     * @return tcp://192.168.0.1/path?user=1&path=2
     */
    URI uri();

    /**
     * 获取参数集合
     */
    Map<String, String> paramMap();

    /**
     * 获取参数
     *
     * @param name 参数名
     */
    String param(String name);

    /**
     * 获取参数或默认值
     *
     * @param name 参数名
     * @param def  默认值
     */
    String paramOrDefault(String name, String def);

    /**
     * 设置或修改参数
     */
    void param(String name, String value);
}
