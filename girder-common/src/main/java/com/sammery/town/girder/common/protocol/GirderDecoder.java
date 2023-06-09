package com.sammery.town.girder.common.protocol;

import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import com.sammery.town.girder.common.utils.P698Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * @author forose
 */
@Slf4j
public class GirderDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {

        int readableBytes = msg.readableBytes();
        byte[] bytes = new byte[readableBytes];
        msg.readBytes(bytes);
        //打印出已经处理好的完整的数据内容
        log.debug(ctx.channel() + " <- " + CommUtil.byteToHexStringWithBlank(bytes));
        if (!P698Util.verifyFcs(bytes)) {
            return;
        }
        out.add(transMessage(bytes));
    }

    private GirderMessage transMessage(byte[] bytes) {
        GirderMessage msg = new GirderMessage();
        int length = ((bytes[4] & 0xFF) << 24) + ((bytes[3] & 0xFF) << 16) + ((bytes[2] & 0xFF) << 8) + (bytes[1] & 0xFF);
        msg.setCmd(bytes[5]);
        msg.setSn((short) (((bytes[6] & 0xFF) << 8) + (bytes[7] & 0xFF)));
        msg.setData(Arrays.copyOfRange(bytes, 8, length - 1));
        return msg;
    }
}