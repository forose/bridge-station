package com.sammery.town.girder.server.handler;

import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.utils.CommUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

import static com.sammery.town.girder.common.consts.MessageType.*;

/**
 * 消息处理器
 * @author 沙漠渔
 */
@Slf4j
public class ServerHandler extends ChannelInboundHandlerAdapter {
    /**
     * 通道关闭
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
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        GirderMessage message = (GirderMessage) msg;
        switch (message.getType()){
            case AUTH:
                authMessageHandler(ctx,message);
                break;
            case CONNECT:
                connectMessageHandler(ctx,message);
                break;
            case HEART:
                heartMessageHandler(ctx,message);
                break;
            default:
                log.warn("未支持的消息类型!");
                break;
        }
    }

    private void authMessageHandler(ChannelHandlerContext ctx,GirderMessage msg){
        String data = CommUtil.byteToHexString(msg.getData());
        if ("11".equals(data)){
            log.info("校验通过");
            // 组织其可以使用的端口
            String hex = Integer.toHexString(18080);
            msg.setType(AUTH);
            msg.setData(CommUtil.hex2Binary(hex));
            ctx.channel().writeAndFlush(msg);
        }
    }

    private void connectMessageHandler(ChannelHandlerContext ctx,GirderMessage msg){

    }

    private void heartMessageHandler(ChannelHandlerContext ctx,GirderMessage msg){
        log.info("Channel Heart : " + ctx.channel());
        ctx.channel().writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        if(channel.isActive()) {
            ctx.close();
        }
    }

}
