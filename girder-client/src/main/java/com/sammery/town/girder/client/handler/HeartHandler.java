package com.sammery.town.girder.client.handler;

import com.sammery.town.girder.common.consts.Command;
import com.sammery.town.girder.common.domain.GirderMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳检测处理器
 *
 * @author 沙漠渔
 */
@Slf4j
public class HeartHandler extends IdleStateHandler {

    public HeartHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (evt.state() == IdleState.WRITER_IDLE) {
            GirderMessage message = new GirderMessage();
            message.setCmd(Command.HEART);
            ctx.channel().writeAndFlush(message);
        }
    }
}
