package com.sammery.town.girder.server.handler;

import com.sammery.town.girder.common.consts.Command;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.server.model.ServiceEntity;
import com.sammery.town.girder.server.station.BridgeStation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

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
            // 1- 在创建连接的时候把管理通道和数据通道做个关系维护
            // 2- 在断开时对关系维护了的数据通道做统一断开处理
        } else {
            Channel stationChannel = bridgeChannel.attr(Constants.NEXT_CHANNEL).get();
            if (stationChannel != null && stationChannel.isActive()) {
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
                break;
        }
    }

    private void authMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        Channel channel = ctx.channel();
        // 获取数据域的内容
        String[] datas = new String(msg.getData(),StandardCharsets.UTF_8).split(",");
        // 判断数据域是否符合要求
        if (datas.length == 2){
            // 判断数据库中是否有对应链接参数的配置服务
            List<ServiceEntity> services =  station.obtainService(datas[1],datas[0]);
            // 如果没有服务 就把该链接断掉
            if (services.size() == 0){
                ctx.close();
            }
            // 否则就返回请求到的服务数据
            msg.setCmd(AUTH);
            msg.setData(services.stream()
                    .map(x->x.getHost().concat(":").concat(String.valueOf(x.getPort())))
                    .collect(Collectors.joining(","))
                    .getBytes(StandardCharsets.UTF_8)
            );
            // 标志当前通道为管理通道
            channel.attr(Constants.MANAGE_CHANNEL).set(true);
            // 发送回复消息 确认鉴权完成
            channel.writeAndFlush(msg);
        }
    }

    private void connectMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        String key = new String(msg.getData(), StandardCharsets.UTF_8);
        Channel bridgeChannel = ctx.channel();
        if (bridgeChannel.hasAttr(Constants.MANAGE_CHANNEL) && bridgeChannel.attr(Constants.MANAGE_CHANNEL).get()) {
            // 如果是控制通道上发过来的连接消息则去连接后端服务
            String[] lan = key.split("@")[1].split(":");
            if (lan[0].equals(bridgeChannel.remoteAddress().toString().substring(1).split(":")[0])){
                return;
            }
            station.link(lan[0], Integer.parseInt(lan[1]), channel -> {
                if (channel != null) {
                    channel.config().setAutoRead(false);
                }
                station.bind(key, channel, true);
            });
        } else {
            String[] lan = key.split("@")[1].split(":");
            if (lan[0].equals(bridgeChannel.remoteAddress().toString().substring(1).split(":")[0])){
                GirderMessage message = new GirderMessage();
                message.setCmd(Command.DISCON);
                bridgeChannel.writeAndFlush(message);
                return;
            }
            station.bind(key, bridgeChannel, false);
        }
    }

    private void disconnectMessageHandler(ChannelHandlerContext ctx) {
        log.warn("远端连接断开: " + ctx.channel());
        Channel bridgeChannel = ctx.channel();
        Channel stationChannel = bridgeChannel.attr(Constants.NEXT_CHANNEL).get();
        if (stationChannel != null && stationChannel.isActive()) {
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
