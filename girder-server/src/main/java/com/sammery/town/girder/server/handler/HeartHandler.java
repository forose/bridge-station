package com.sammery.town.girder.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳检测
 * @author 沙漠渔
 */
@Slf4j
public class HeartHandler extends IdleStateHandler {

    public HeartHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT == evt || IdleStateEvent.READER_IDLE_STATE_EVENT == evt) {
            log.warn("channel read timeout {}", ctx.channel());
            ctx.channel().close();
        }
    }
}
