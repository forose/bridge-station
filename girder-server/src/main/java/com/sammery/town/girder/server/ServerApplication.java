package com.sammery.town.girder.server;

import com.sammery.town.girder.common.protocol.GirderDecoder;
import com.sammery.town.girder.common.protocol.GirderEncoder;
import com.sammery.town.girder.server.handler.HeartHandler;
import com.sammery.town.girder.server.handler.ServerHandler;
import com.sammery.town.girder.server.properties.ServerProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.ByteOrder;

/**
 * 入口
 * @author 沙漠渔
 */
@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class ServerApplication {
    private final ServerProperties serverProperties;
    /**
     * 负责连接请求
     */
    private EventLoopGroup bossGroup;
    /**
     * 负责事件响应
     */
    private EventLoopGroup workerGroup;

    private Channel channel;

    @PostConstruct
    public void initChannel() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(serverProperties.getBoss());
        workerGroup = new NioEventLoopGroup(serverProperties.getWorker());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        // 添加心跳处理
                        ch.pipeline().addLast(new HeartHandler(30,0,0));
                        // 添加出站编码器
                        ch.pipeline().addLast(new GirderEncoder());

                        // 添加长度解码器
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN,1024 * 8,1,2,-1,0,true));
                        ch.pipeline().addLast(new GirderDecoder());
                        ch.pipeline().addLast(new ServerHandler());
                    }
                });

        ChannelFuture channelFuture = bootstrap.bind(serverProperties.getPort()).sync();
        channel = channelFuture.channel();
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("Girder Server have success bind to " + serverProperties.getPort());
            } else {
                log.error("Fail bind to " + serverProperties.getPort());
            }
        });
    }

    @PreDestroy
    public void destroyChannel() {
        if (channel != null){
            channel.close();
        }
        if (workerGroup != null){
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
