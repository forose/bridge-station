package com.sammery.town.girder.client.handler;

import com.sammery.town.girder.client.station.BridgeStation;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.sammery.town.girder.common.consts.Command.*;

/**
 * 本地消息处理器 其与代理消息处理器通过桥头堡相连
 *
 * @author 沙漠渔
 */
@Slf4j
@RequiredArgsConstructor
public class StationHandler extends ChannelInboundHandlerAdapter {

    /**
     * 桥头堡 两边的联络工具
     */
    private final BridgeStation station;

    /**
     * 通道关闭
     *
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("本地连接断开: " + ctx.channel());
        Channel stationChannel = ctx.channel();
        Channel bridgeChannel = stationChannel.attr(Constants.NEXT_CHANNEL).get();
        if (bridgeChannel != null){
            // 如果是待归还的连接 表明远端已经断开，直接将通道连接归还即可
            if (bridgeChannel.hasAttr(Constants.STATUS_RETURN) && bridgeChannel.attr(Constants.STATUS_RETURN).get()){
                station.returnChanel(bridgeChannel);
            }else {
                // 否则需要告知远端 自己已经断开,断开服务端对应连接
                GirderMessage message = new GirderMessage();
                message.setCmd(DISCON);
                bridgeChannel.writeAndFlush(message);
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
        log.info("本地连接建立: " + ctx.channel());
        Channel stationChannel = ctx.channel();
        stationChannel.config().setAutoRead(false);
        // 通道建立完成之后进行远端绑定
        GirderMessage message = new GirderMessage();
        message.setCmd(CONNECT);
        String addr = stationChannel.localAddress().toString().substring(1);
        message.setData((ctx.channel().id().asShortText() + "@" + addr).getBytes());
        // 建立连接之后 建立代理通道 连接至服务端
        // 这里采用异步处理的方式
        // 1.通过底层连接告知服务端去建对端连接
        // 2.获取到通道连接再次发送相同的连接信息进行远程连接绑定
        // 3.远程连接绑定完毕之后在通道连接中可收到连接消息，设置通道可读
        station.borrowChannel(bridgeChannel -> {
            if (bridgeChannel != null) {
                // 将代理通道添加至当前通道的下一跳
                stationChannel.attr(Constants.NEXT_CHANNEL).set(bridgeChannel);
                // 将当前通道添加至代理通道的下一跳
                bridgeChannel.attr(Constants.NEXT_CHANNEL).set(stationChannel);
                bridgeChannel.writeAndFlush(message);
            } else {
                ctx.close();
            }
        });
        station.active(message);
    }

    /**
     * 消息读取
     *
     * @param ctx 上下文
     * @param msg 消息内容
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.info("本地连接消息: " + ctx.channel());
        Channel stationChannel = ctx.channel();
        Channel bridgeChannel = stationChannel.attr(Constants.NEXT_CHANNEL).get();
        GirderMessage message = new GirderMessage();
        message.setCmd(TRANSFER);
        message.setData((byte[]) msg);
        bridgeChannel.writeAndFlush(message);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel stationChannel = ctx.channel();
        Channel bridgeChannel = stationChannel.attr(Constants.NEXT_CHANNEL).get();
        if (bridgeChannel != null) {
            bridgeChannel.config().setAutoRead(stationChannel.isWritable());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }

}
