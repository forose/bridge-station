package com.sammery.town.girder.server.handler;

import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import com.sammery.town.girder.server.station.BridgeStation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.net.SocketAddress;

import static com.sammery.town.girder.common.consts.Command.*;

/**
 * 消息处理器
 *
 * @author 沙漠渔
 */
@Slf4j
@RequiredArgsConstructor
public class ServerHandler extends ChannelInboundHandlerAdapter {
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
            // todo 主通道断开的话 把该终端的私有连接都给断开  暂时未做
        }else {
            Channel stationChannel = bridgeChannel.attr(Constants.NEXT_CHANNEL).get();
            if (stationChannel != null && stationChannel.isActive()){
                stationChannel.close();
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("通道连接建立: " + ctx.channel());
    }

    /**
     * 消息读取
     *
     * @param ctx
     * @param msg
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
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
            case HEART:
                heartMessageHandler(ctx, message);
                break;
            default:
                log.warn("未支持的消息类型!");
                break;
        }
    }

    private void authMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        String data = CommUtil.byteToHexString(msg.getData());
        if ("11".equals(data)) {
            log.info("校验通过");
            // 组织其可以使用的端口
            String hex = Integer.toHexString(18080);
            msg.setCmd(AUTH);
            msg.setData(CommUtil.hex2Binary(hex));
            ctx.channel().writeAndFlush(msg);
            ctx.channel().attr(Constants.MANAGE_CHANNEL).set(true);
        }
    }

    private void connectMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        String key = new String(msg.getData());
        Channel bridgeChannel = ctx.channel();
        if (bridgeChannel.hasAttr(Constants.MANAGE_CHANNEL) && bridgeChannel.attr(Constants.MANAGE_CHANNEL).get()) {
            // 如果是控制通道上发过来的连接消息则去连接后端服务
            String[] lan = key.split("@")[1].split(":");
            station.link("192.168.0.107", 6600, channel -> {
                if (channel != null) {
                    channel.config().setAutoRead(false);
                    channel.attr(Constants.CHANNEL_KEY).set(key);
                    station.bind(key, channel, true);
                } else {
                    station.bind(key, null, true);
                }
            });
        } else {
            ctx.channel().attr(Constants.CHANNEL_KEY).set(key);
            station.bind(key, ctx.channel(), false);
        }
    }

    private void disconnectMessageHandler(ChannelHandlerContext ctx) {
        log.warn("通道断开消息: " + ctx.channel());
        Channel bridgeChannel = ctx.channel();
        Channel stationChannel = bridgeChannel.attr(Constants.NEXT_CHANNEL).get();
        if (stationChannel != null) {
            stationChannel.close();
        }
    }

    private void transferMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        log.info("通道数据消息: " + ctx.channel());
        Channel stationChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (stationChannel != null) {
            stationChannel.writeAndFlush(msg.getData());
        }
    }

    private void heartMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        ctx.channel().writeAndFlush(msg);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
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
