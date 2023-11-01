package org.noear.socketd.broker.netty.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.noear.socketd.core.Codec;
import org.noear.socketd.core.CodecByteBuffer;
import org.noear.socketd.core.Frame;

import java.nio.ByteBuffer;

/**
 * @author noear
 * @since 2.0
 */
public class NettyMessageEncoder extends MessageToByteEncoder<Frame> {
    private final Codec<ByteBuffer> codec;

    public NettyMessageEncoder(Codec<ByteBuffer> codec) {
        this.codec = codec;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Frame message, ByteBuf byteBuf) throws Exception {
        if (message != null) {
            ByteBuffer buf = codec.encode(message);
            byteBuf.writeBytes(buf.array());
        }
    }
}
