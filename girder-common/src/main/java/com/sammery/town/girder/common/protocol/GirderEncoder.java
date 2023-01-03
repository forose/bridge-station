package com.sammery.town.girder.common.protocol;

import com.sammery.town.girder.common.consts.MessageType;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import com.sammery.town.girder.common.utils.P698Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.Arrays;

import static io.netty.buffer.Unpooled.buffer;

/**
 * @author forose
 */
public class GirderEncoder extends MessageToByteEncoder<GirderMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, GirderMessage msg, ByteBuf out) throws Exception {
        int length = 2 + 1 + 2 + 2 + (msg.getData() == null ? 0 : msg.getData().length);
        out.writeByte(0x68);
        out.writeByte(length & 0xFF);
        out.writeByte((length >> 8) & 0xFF);
        out.writeByte(msg.getType());
        out.writeShort(msg.getSn());
        out.writeBytes(msg.getData() == null ? new byte[0] : msg.getData());
        byte[] now = Arrays.copyOf(out.array(),length - 1);
        out.writeBytes(P698Util.obtainCrc(now));
        out.writeByte(0x16);
    }
}