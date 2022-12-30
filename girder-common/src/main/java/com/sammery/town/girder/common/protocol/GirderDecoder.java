package com.sammery.town.girder.common.protocol;

import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import com.sammery.town.girder.common.utils.P698Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import lombok.extern.slf4j.Slf4j;
import sun.nio.cs.ext.MS874;

import java.util.Arrays;
import java.util.List;

/**
 * @author forose
 */
@Slf4j
public class GirderDecoder extends ByteArrayDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {

        int readableBytes = msg.readableBytes();
        byte[] bytes = new byte[readableBytes];
        msg.readBytes(bytes);
        //打印出已经处理好的完整的数据内容
        log.debug(CommUtil.byteToHexStringWithBlank(bytes));
        out.add(obtainTransMessage(bytes));
    }

    private static GirderMessage obtainTransMessage(byte[] bytes) {
        GirderMessage msg = new GirderMessage();
        msg.setHead(bytes[0]);
        msg.setLength(((bytes[2]<<8) & 0xFF) + (bytes[1] & 0xFF));
        msg.setType(bytes[3]);
        msg.setSn(((bytes[4]<<8) & 0xFF) + (bytes[5] & 0xFF));
        msg.setData(Arrays.copyOfRange(bytes,6,msg.getLength() - 1));
        msg.setCrc(P698Util.obtainCrc(Arrays.copyOf(bytes,msg.getLength() - 1)));
        msg.setTail(bytes[msg.getLength() + 1]);
        return msg;
    }
}