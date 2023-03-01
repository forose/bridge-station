package com.sammery.town.girder.server.handler;

import com.sammery.town.girder.common.consts.Command;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.server.model.AccessEntity;
import com.sammery.town.girder.server.model.PersonEntity;
import com.sammery.town.girder.server.model.ServiceEntity;
import com.sammery.town.girder.server.station.BridgeStation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundInvoker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
        Channel bridgeChannel = ctx.channel();
        if (bridgeChannel.hasAttr(Constants.MANAGE_CHANNEL) && bridgeChannel.attr(Constants.MANAGE_CHANNEL).get()) {
            log.warn("管理通道关闭: " + ctx.channel());
            bridgeChannel.attr(Constants.SLAVE_CHANNEL).get()
                    .forEach(ChannelOutboundInvoker::close);
        } else {
            log.warn("数据通道关闭: " + ctx.channel());
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
        channel.attr(Constants.SLAVE_CHANNEL).set(new ArrayList<>());
        // 获取数据域的内容
        String[] datas = new String(msg.getData(), StandardCharsets.UTF_8).split(",");
        // 判断数据域是否符合要求
        if (datas.length == 2) {
            PersonEntity person = station.obtainPerson(datas[1], datas[0]);
            // 判断数据库中是否有对应链接参数的配置服务
            List<ServiceEntity> services = station.obtainService(person);
            // 如果没有服务 就把该链接断掉
            if (services.size() == 0) {
                ctx.close();
            }
            String servicesString = services.stream()
                    .map(x -> x.getHost().concat(":").concat(String.valueOf(x.getPort())))
                    .collect(Collectors.joining(","));
            // 否则就返回请求到的服务数据
            msg.setCmd(AUTH);
            msg.setData(servicesString.getBytes(StandardCharsets.UTF_8));
            // 标志当前通道为管理通道
            channel.attr(Constants.MANAGE_CHANNEL).set(true);
            // 将该管理通道存在的服务记录下来 方便后续在connectMessageHandler中进行校验使用。
            channel.attr(Constants.INNER_SERVICES).set(servicesString);
            channel.attr(Constants.CHANNEL_HOLDER).set(person.getName());
            // 发送回复消息 确认鉴权完成
            channel.writeAndFlush(msg);
        } else {
            channel.close();
        }
    }

    private void connectMessageHandler(ChannelHandlerContext ctx, GirderMessage msg) {
        // 建立连接请求的格式 id@ip:port
        String key = new String(msg.getData(), StandardCharsets.UTF_8);
        Channel bridgeChannel = ctx.channel();
        if (bridgeChannel.hasAttr(Constants.MANAGE_CHANNEL) && bridgeChannel.attr(Constants.MANAGE_CHANNEL).get()) {
            // 如果是控制通道上发过来的连接消息则去连接后端服务
            // 如果发过来的请求服务不在服务列表里，则直接将管理通道断掉，说明有坏想法
            String lanStr = key.split("@")[1];
            if (!bridgeChannel.hasAttr(Constants.INNER_SERVICES) || !bridgeChannel.attr(Constants.INNER_SERVICES).get().contains(lanStr)) {
                station.bind(key, bridgeChannel, null);
                return;
            }
            // 验证通过了之后 判断是否是去请求自己的服务 如果是则直接return掉 避免陷入死循环（这里待确认是否还有其他情况）
            String[] lan = lanStr.split(":");
            if (lan[0].equals(bridgeChannel.remoteAddress().toString().substring(1).split(":")[0])) {
                station.bind(key, bridgeChannel, null);
                return;
            }
            // 否验证通过了则进入后端的服务连接过程
            station.link(lan[0], Integer.parseInt(lan[1]), channel -> {
                if (channel != null) {
                    channel.config().setAutoRead(false);
                    if (bridgeChannel.hasAttr(Constants.CHANNEL_HOLDER)) {
                        log.info("内部服务建立：" + bridgeChannel + " - " + bridgeChannel.attr(Constants.CHANNEL_HOLDER).get());
                        AccessEntity access = new AccessEntity();
                        access.setPerson(bridgeChannel.attr(Constants.CHANNEL_HOLDER).get());
                        access.setService(channel.remoteAddress().toString().substring(1));
                        access.setRemote(((InetSocketAddress) bridgeChannel.remoteAddress()).getAddress().getHostName());
                        station.saveAccess(access);
                    }
                }
                station.bind(key, bridgeChannel, channel);
            });
        } else {
            String[] lan = key.split("@")[1].split(":");
            if (lan[0].equals(bridgeChannel.remoteAddress().toString().substring(1).split(":")[0])) {
                GirderMessage message = new GirderMessage();
                message.setCmd(Command.DISCON);
                bridgeChannel.writeAndFlush(message);
                return;
            }
            station.bind(key, bridgeChannel, null);
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
