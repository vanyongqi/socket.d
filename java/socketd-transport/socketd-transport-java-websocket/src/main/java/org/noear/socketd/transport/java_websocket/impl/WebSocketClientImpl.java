package org.noear.socketd.transport.java_websocket.impl;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.noear.socketd.exception.SocketdConnectionException;
import org.noear.socketd.transport.client.ClientHandshakeResult;
import org.noear.socketd.transport.core.ChannelInternal;
import org.noear.socketd.transport.core.Flags;
import org.noear.socketd.transport.java_websocket.WsNioClient;
import org.noear.socketd.transport.core.Frame;
import org.noear.socketd.transport.core.internal.ChannelDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class WebSocketClientImpl extends WebSocketClient {
    static final Logger log = LoggerFactory.getLogger(WebSocketClientImpl.class);
    private WsNioClient client;
    private ChannelInternal channel;
    private CompletableFuture<ClientHandshakeResult> handshakeFuture;

    public WebSocketClientImpl(URI serverUri, WsNioClient client) {
        super(serverUri);
        this.client = client;
        this.channel = new ChannelDefault<>(this, client);
    }

    public CompletableFuture<ClientHandshakeResult> getHandshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void onWebsocketPing(WebSocket conn, Framedata f) {
        //用于支持 socket.d 控制 idleTimeout //关闭自动 ping->Pong
        //super.onWebsocketPing(conn, f);
    }

    @Override
    public void connect() {
        this.handshakeFuture = new CompletableFuture<>();
        super.connect();
    }

    @Override
    public void reconnect() {
        this.handshakeFuture = new CompletableFuture<>();
        super.reconnect();
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        try {
            channel.sendConnect(client.getConfig().getUrl());
        } catch (Throwable e) {
            if (log.isWarnEnabled()) {
                log.warn("Client channel sendConnect error", e);
            }
        }
    }

    @Override
    public void onMessage(String test) {
        //普通 websocket 握手都通不过
        if (log.isWarnEnabled()) {
            log.warn("Client channel unsupported onMessage(String test)");
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            Frame frame = client.getAssistant().read(bytes);

            if (frame != null) {
                if (frame.flag() == Flags.Connack) {
                    channel.onOpenFuture((r, e) -> {
                        if (e == null) {
                            handshakeFuture.complete(new ClientHandshakeResult(channel, null));
                        } else {
                            handshakeFuture.completeExceptionally(e);
                        }
                    });
                }

                client.getProcessor().onReceive(channel, frame);
            }
        } catch (Exception e) {
            if (e instanceof SocketdConnectionException) {
                //说明握手失败了
                handshakeFuture.complete(new ClientHandshakeResult(channel, e));
                return;
            }

            if (log.isWarnEnabled()) {
                log.warn("WebSocket client onMessage error", e);
            }
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        client.getProcessor().onClose(channel);
    }

    @Override
    public void onError(Exception e) {
        client.getProcessor().onError(channel, e);
    }
}
