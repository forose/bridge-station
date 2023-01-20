package com.sammery.town.girder.common.protocol;

import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import com.sammery.town.girder.common.utils.P698Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息编码器
 * @author 沙漠渔
 */
@Slf4j
public class StationEncoder extends MessageToByteEncoder<byte[]> {

    private static final AtomicInteger SERIAL = new AtomicInteger(1);
    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] bytes, ByteBuf out) {
        out.writeBytes(bytes);
    }
}