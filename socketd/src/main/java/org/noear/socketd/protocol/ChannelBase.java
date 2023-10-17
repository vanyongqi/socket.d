package org.noear.socketd.protocol;

import org.noear.socketd.protocol.impl.FrameFactory;

import java.io.IOException;

/**
 * @author noear 2023/10/17 created
 */
public abstract class ChannelBase implements Channel {

    private Handshaker handshaker;
    @Override
    public void setHandshaker(Handshaker handshaker) {
        this.handshaker = handshaker;
    }


    @Override
    public Handshaker getHandshaker() {
        return handshaker;
    }


    @Override
    public void sendConnect(String uri) throws IOException {
        send(FrameFactory.connectFrame(uri));
    }

    @Override
    public void sendConnack() throws IOException {
        send(FrameFactory.connackFrame());
    }

    @Override
    public void sendPing() throws IOException {
        send(FrameFactory.pingFrame());
    }

    @Override
    public void sendPong() throws IOException {
        send(FrameFactory.pongFrame());
    }
}
