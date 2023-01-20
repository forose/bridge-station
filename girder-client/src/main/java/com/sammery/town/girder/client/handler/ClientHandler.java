package com.sammery.town.girder.client.handler;

import com.sammery.town.girder.client.station.BridgeStation;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

import static com.sammery.town.girder.common.consts.MessageType.*;

/**
 * 代理消息处理器 其与本地消息处理器通过桥头堡相连
 * @author 沙漠渔
 */
@Slf4j@RequiredArgsConstructor
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private final BridgeStation station;
    /**
     * 通道关闭
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("通道连接关闭: " + ctx.channel());
    }

    /**
     * 通道建立
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("通道连接建立: " + ctx.channel());
    }

    /**
     * 消息读取
     * @param ctx 上下文
     * @param msg 消息内容
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        GirderMessage message = (GirderMessage) msg;
        switch (message.getType()) {
            case AUTH:
                authMessageHandler(ctx, message);
                break;
            case CONNECT:
                connectMessageHandler(ctx, message);
                break;
            case DISCONNECT:
                disconnectMessageHandler(ctx, message);
                break;
            case TRANSFER:
                transferMessageHandler(ctx, message);
                break;
            default:
                break;
        }
    }

    private void authMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) throws InterruptedException {
        String ports = CommUtil.byteToHexString(msg.getData());
        int port = Integer.parseInt(ports,16);
        log.info("端口:" + port);
        log.info(String.valueOf(ctx.channel().hasAttr(Constants.MANAGE_CHANNEL)));
        station.open(port);
    }

    private void connectMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        log.info("通道连接消息: " + ctx.channel());
        // 客户端收到连接消息则表明服务端绑定已完成，可以把本地连接的自动读取打开了
        ctx.channel().attr(Constants.NEXT_CHANNEL).get().config().setAutoRead(true);
    }

    private void disconnectMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {

    }

    private void transferMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        log.info("通道数据消息: " + ctx.channel());
        Channel stationChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (stationChannel != null) {
            stationChannel.writeAndFlush(msg.getData());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if(ctx.channel().isActive()) {
            ctx.close();
        }
    }

}
