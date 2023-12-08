package org.noear.socketd.transport.core.fragment;

import org.noear.socketd.exception.SocketdCodecException;
import org.noear.socketd.transport.core.EntityMetas;
import org.noear.socketd.transport.core.FragmentAggregator;
import org.noear.socketd.transport.core.Frame;
import org.noear.socketd.transport.core.MessageInternal;
import org.noear.socketd.transport.core.entity.EntityDefault;
import org.noear.socketd.transport.core.internal.MessageDefault;
import org.noear.socketd.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 分片聚合器
 *
 * @author noear
 * @since 2.0
 */
public class FragmentAggregatorTempfile implements FragmentAggregator {
    //主导消息
    private MessageInternal main;
    //数据流大小
    private int dataStreamSize;
    //数据总长度
    private int dataLength;
    //临时文件
    private File tmpfile;
    private FileChannel tmpfileChannel;

    public FragmentAggregatorTempfile(MessageInternal main) throws IOException {
        this.main = main;
        String dataLengthStr = main.meta(EntityMetas.META_DATA_LENGTH);

        if (Utils.isEmpty(dataLengthStr)) {
            throw new SocketdCodecException("Missing '" + EntityMetas.META_DATA_LENGTH + "' meta, event=" + main.event());
        }

        this.dataLength = Integer.parseInt(dataLengthStr);

        this.tmpfile = File.createTempFile(main.sid(), ".tmp");
        this.tmpfileChannel = new RandomAccessFile(tmpfile, "rw").getChannel();
    }

    /**
     * 获取消息流Id（用于消息交互、分片）
     */
    public String getSid() {
        return main.sid();
    }

    /**
     * 数据流大小
     */
    public int getDataStreamSize() {
        return dataStreamSize;
    }

    /**
     * 数据总长度
     */
    public int getDataLength() {
        return dataLength;
    }

    /**
     * 获取聚合后的帧
     */
    public Frame get() throws IOException {
        try {
            MappedByteBuffer dataBuffer = tmpfileChannel
                    .map(FileChannel.MapMode.READ_ONLY, 0, dataLength);

            //返回
            return new Frame(main.flag(), new MessageDefault()
                    .flag(main.flag())
                    .sid(main.sid())
                    .event(main.event())
                    .entity(new EntityDefault().metaMap(main.metaMap()).data(dataBuffer)));
        } finally {
            tmpfileChannel.close();
        }
    }

    /**
     * 添加帧
     */
    public void add(int index, MessageInternal message) throws IOException {
        //添加分片
        tmpfileChannel.write(message.data());
        //添加计数
        dataStreamSize = dataStreamSize + message.dataSize();
    }
}
