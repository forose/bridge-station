package com.sammery.town.girder.server.handler;

import com.sammery.town.girder.server.station.BridgeStation;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.sammery.town.girder.common.consts.MessageType.CONNECT;
import static com.sammery.town.girder.common.consts.MessageType.TRANSFER;

/**
 * 本地消息处理器 其与代理消息处理器通过桥头堡相连
 * @author 沙漠渔
 */
@Slf4j@RequiredArgsConstructor
public class StationHandler extends ChannelInboundHandlerAdapter {

    /**
     * 桥头堡 两边的联络工具
     */
    private final BridgeStation station;
    /**
     * 通道关闭
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("本地连接断开: " + ctx.channel());
    }

    /**
     * 通道建立
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("本地连接建立: " + ctx.channel());
    }

    /**
     * 消息读取
     * @param ctx 上下文
     * @param msg 消息内容
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] bytes = (byte[]) msg;
        log.info("本地连接消息: " + ctx.channel());
        Channel channel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        GirderMessage message = new GirderMessage();
        message.setType(TRANSFER);
        message.setData(bytes);
        channel.writeAndFlush(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if(ctx.channel().isActive()) {
            ctx.close();
        }
    }

}
