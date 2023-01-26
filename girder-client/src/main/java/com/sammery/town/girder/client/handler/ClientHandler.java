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

import static com.sammery.town.girder.common.consts.Command.*;

/**
 * 代理消息处理器 其与本地消息处理器通过桥头堡相连
 *
 * @author 沙漠渔
 */
@Slf4j
@RequiredArgsConstructor
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private final BridgeStation station;

    /**
     * 通道关闭
     *
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("通道连接关闭: " + ctx.channel());
        Channel bridgeChannel = ctx.channel();
        if (bridgeChannel.hasAttr(Constants.MANAGE_CHANNEL) && bridgeChannel.attr(Constants.MANAGE_CHANNEL).get()) {
            station.link();
        }else {
            Channel stationChannel = bridgeChannel.attr(Constants.NEXT_CHANNEL).get();
            if (stationChannel != null && stationChannel.isActive()){
                stationChannel.close();
            }
        }
    }

    /**
     * 通道建立
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("通道连接建立: " + ctx.channel());
    }

    /**
     * 消息读取
     *
     * @param ctx 上下文
     * @param msg 消息内容
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        GirderMessage message = (GirderMessage) msg;
        switch (message.getCmd()) {
            case AUTH:
                authMessageHandler(ctx, message);
                break;
            case CONNECT:
                connectMessageHandler(ctx, message);
                break;
            case DISCON:
                disconnectMessageHandler(ctx);
                break;
            case TRANSFER:
                transferMessageHandler(ctx, message);
                break;
            default:
                break;
        }
    }

    private void authMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        String ports = CommUtil.byteToHexString(msg.getData());
        int port = Integer.parseInt(ports, 16);
        log.info("端口:" + port);
        station.open(port);
    }

    private void connectMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        log.info("通道连接消息: " + ctx.channel());
        Channel bridgeChannel = ctx.channel();
        Channel stationChannel = bridgeChannel.attr(Constants.NEXT_CHANNEL).get();
        if (stationChannel != null) {
            // 客户端收到连接消息则表明服务端绑定已完成，可以把本地连接的自动读取打开了
            stationChannel.config().setAutoRead(true);
        }
    }

    private void disconnectMessageHandler(ChannelHandlerContext ctx) {
        log.warn("通道断开消息: " + ctx.channel());
        Channel bridgeChannel = ctx.channel();
        Channel stationChannel = bridgeChannel.attr(Constants.NEXT_CHANNEL).get();
        if (stationChannel != null && stationChannel.isActive()) {
            stationChannel.close();
            // 将通道连接置为待归还状态 用于在本地连接断开之后立即归还通道连接
            bridgeChannel.attr(Constants.STATUS_RETURN).set(true);
        }else {
            // 归还持有的数据连接通道
            station.returnChanel(bridgeChannel);
        }
    }

    private void transferMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        log.info("通道数据消息: " + ctx.channel());
        Channel bridgeChannel = ctx.channel();
        Channel stationChannel = bridgeChannel.attr(Constants.NEXT_CHANNEL).get();
        if (stationChannel != null) {
            stationChannel.writeAndFlush(msg.getData());
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel bridgeChannel = ctx.channel();
        Channel stationChannel = bridgeChannel.attr(Constants.NEXT_CHANNEL).get();
        if (stationChannel != null) {
            stationChannel.config().setAutoRead(bridgeChannel.isWritable());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }

}
