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
        log.debug(ctx.channel() + " <- " + CommUtil.byteToHexStringWithBlank(bytes));
        if (!P698Util.verifyFcs(bytes)){
            return;
        }
        out.add(transMessage(bytes));
    }

    private GirderMessage transMessage(byte[] bytes) {
        GirderMessage msg = new GirderMessage();
        int length = ((bytes[2]<<8) & 0xFF) + (bytes[1] & 0xFF);
        msg.setCmd(bytes[3]);
        msg.setSn((short) (((bytes[4]<<8) & 0xFF00) + (bytes[5] & 0xFF)));
        msg.setData(Arrays.copyOfRange(bytes,6,length - 1));
        return msg;
    }
}