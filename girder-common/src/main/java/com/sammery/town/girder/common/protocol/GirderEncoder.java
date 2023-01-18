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
public class GirderEncoder extends MessageToByteEncoder<GirderMessage> {

    private static final AtomicInteger SERIAL = new AtomicInteger(1);
    @Override
    protected void encode(ChannelHandlerContext ctx, GirderMessage msg, ByteBuf out) {
        int length = 2 + 1 + 2 + 2 + (msg.getData() == null ? 0 : msg.getData().length);
        short sn = msg.getSn();

        ByteBuffer byteBuffer = ByteBuffer.allocate(length + 2);
        byteBuffer.put((byte) 0x68);
        byteBuffer.put((byte) (length & 0xFF));
        byteBuffer.put((byte) ((length >> 8) & 0xFF));
        byteBuffer.put(msg.getType());
        byteBuffer.putShort(sn == 0 ? (short) (SERIAL.getAndIncrement() & 0xFFFF) : sn);
        byteBuffer.put(msg.getData() == null ? new byte[0] : msg.getData());
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x16);
        byte[] crc = P698Util.obtainFcs(byteBuffer.array());
        byteBuffer.put(length - 1,crc[0]);
        byteBuffer.put(length, crc[1]);
        byte[] send = byteBuffer.array();
        log.debug(CommUtil.byteToHexStringWithBlank(send));
        out.writeBytes(send);
    }
}