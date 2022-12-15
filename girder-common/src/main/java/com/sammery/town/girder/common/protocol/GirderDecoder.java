package com.sammery.town.girder.common.protocol;

import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import lombok.extern.slf4j.Slf4j;

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

    private GirderMessage obtainTransMessage(byte[] bytes) {
        return new GirderMessage();
    }
}