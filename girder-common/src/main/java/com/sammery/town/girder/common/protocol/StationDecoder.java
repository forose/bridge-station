package com.sammery.town.girder.common.protocol;

import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import com.sammery.town.girder.common.utils.P698Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * @author 沙漠渔
 */
@Slf4j
public class StationDecoder extends ByteArrayDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {

        int readableBytes = msg.readableBytes();
        byte[] bytes = new byte[readableBytes];
        msg.readBytes(bytes);
        out.add(bytes);
    }
}