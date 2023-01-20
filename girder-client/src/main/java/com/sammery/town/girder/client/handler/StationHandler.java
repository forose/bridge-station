package com.sammery.town.girder.client.handler;

import com.sammery.town.girder.client.station.BridgeStation;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.sammery.town.girder.common.consts.MessageType.*;

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
        station.returnChanel(ctx.channel().attr(Constants.NEXT_CHANNEL).get());
    }

    /**
     * 通道建立
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("本地连接建立: " + ctx.channel());
        ctx.channel().config().setAutoRead(false);
        // 建立连接之后 建立代理通道 连接至服务端
        // 这里采用异步处理的方式
        // 1.通过底层连接告知服务端去建对端连接
        // 2.获取到通道连接再次发送相同的连接信息进行远程连接绑定
        // 3.远程连接绑定完毕之后在通道连接中可收到连接消息，设置通道可读
        station.borrowChannel(channel -> {
            if (channel != null){
                // 将代理通道添加至当前通道的下一跳
                ctx.channel().attr(Constants.NEXT_CHANNEL).set(channel);
                // 将当前通道添加至代理通道的下一跳
                channel.attr(Constants.NEXT_CHANNEL).set(ctx.channel());
                // 通道建立完成之后进行远端绑定
                GirderMessage message = new GirderMessage();
                message.setType(CONNECT);
                String addr = ctx.channel().localAddress().toString().substring(1);
                message.setData((ctx.channel().id().asShortText() + "@" + addr).getBytes());
                channel.writeAndFlush(message);
            }else {
                ctx.close();
            }
        });
        station.active(ctx);
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
