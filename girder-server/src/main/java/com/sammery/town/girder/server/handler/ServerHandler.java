package com.sammery.town.girder.server.handler;

import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.listener.ChannelListener;
import com.sammery.town.girder.common.utils.CommUtil;
import com.sammery.town.girder.server.station.BridgeStation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

import static com.sammery.town.girder.common.consts.MessageType.*;

/**
 * 消息处理器
 *
 * @author 沙漠渔
 */
@Slf4j@RequiredArgsConstructor
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private final BridgeStation station;
    /**
     * 通道关闭
     *
     * @param ctx 上下文
     * @throws Exception 异常
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("Channel Inactive : " + ctx.channel());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel Active : " + ctx.channel());
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
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
            msg.setType(AUTH);
            msg.setData(CommUtil.hex2Binary(hex));
            ctx.channel().writeAndFlush(msg);
            ctx.channel().attr(Constants.MANAGE_CHANNEL).set(true);
        }
    }

    private void connectMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) throws Exception {
        String key = new String(msg.getData());
        if (ctx.channel().hasAttr(Constants.MANAGE_CHANNEL)){
            // 如果是空值通道上发过来的连接消息则去连接后端服务
            String[] lan = key.split("@")[1].split(":");
            station.link("192.168.0.107",6600, channel -> {
                if (channel != null){
                    channel.attr(Constants.CHANNEL_KEY).set(key);
                    station.bind(channel,true);
                }
            });
        }else {
            ctx.channel().attr(Constants.CHANNEL_KEY).set(key);
            station.bind(ctx.channel(),false);
        }
    }

    private void disconnectMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {

    }

    private void transferMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        log.info("通道数据消息: " + ctx.channel());
        Channel stationChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (stationChannel != null) {
            stationChannel.writeAndFlush(msg.getData());
        }else {
            // 发送断开的消息
        }
    }

    private void heartMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        log.info("Channel Heart : " + ctx.channel());
        ctx.channel().writeAndFlush(msg);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            ctx.close();
        }
    }

}
