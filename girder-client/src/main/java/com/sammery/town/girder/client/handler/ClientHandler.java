package com.sammery.town.girder.client.handler;

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
public class ClientHandler extends ChannelInboundHandlerAdapter {
    /**
     * 通道关闭
     * @param ctx 上下文
     * @throws Exception 异常
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel Inactive : " + ctx.channel());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel Active : " + ctx.channel());
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
                log.info("鉴权");
                break;
            case CONNECT:
                log.info("连接");
                break;
            case HEART:
                log.info("心跳");
                break;
            default:
                log.warn("未支持的消息类型!");
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if(ctx.channel().isActive()) {
            ctx.close();
        }
    }

}
