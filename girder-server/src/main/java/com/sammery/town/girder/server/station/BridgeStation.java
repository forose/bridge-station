package com.sammery.town.girder.server.station;

import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.consts.Command;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.listener.ChannelListener;
import com.sammery.town.girder.common.protocol.GirderDecoder;
import com.sammery.town.girder.common.protocol.GirderEncoder;
import com.sammery.town.girder.common.protocol.StationDecoder;
import com.sammery.town.girder.common.protocol.StationEncoder;
import com.sammery.town.girder.server.handler.HeartHandler;
import com.sammery.town.girder.server.handler.ServerHandler;
import com.sammery.town.girder.server.handler.StationHandler;
import com.sammery.town.girder.server.properties.ServerProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BridgeStation {
    private final ServerProperties serverProperties;
    /**
     * 负责连接请求
     */
    private EventLoopGroup bossGroup;
    /**
     * 负责事件响应
     */
    private EventLoopGroup workerGroup;

    private EventLoopGroup stationGroup;

    private Channel channel;

    private final Map<String, Channel> binding = new ConcurrentHashMap<>();

    public synchronized void bind(String key, Channel channel, boolean station) {
        Channel has = binding.get(key);
        if (has != null) {
            if (channel != null) {
                has.attr(Constants.NEXT_CHANNEL).set(channel);
                channel.attr(Constants.NEXT_CHANNEL).set(has);
                GirderMessage message = new GirderMessage();
                message.setCmd(Command.CONNECT);
                message.setData(key.getBytes());
                if (station) {
                    has.writeAndFlush(message);
                    channel.config().setAutoRead(true);
                } else {
                    channel.writeAndFlush(message);
                    has.config().setAutoRead(true);
                }
            } else {
                GirderMessage message = new GirderMessage();
                message.setCmd(Command.DISCON);
                message.setData(key.getBytes());
                has.writeAndFlush(message);
            }
        } else {
            binding.put(key, channel);
        }
    }

    public void link(String ip, int port, final ChannelListener listener) {
        new Bootstrap().group(stationGroup).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast(new StationDecoder());
                        channel.pipeline().addLast(new StationEncoder());
                        channel.pipeline().addLast(new StationHandler(BridgeStation.this));
                    }
                }).connect(ip, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("连接服务端成功: {}", future.channel());
                listener.complete(future.channel());
            } else {
                log.warn("连接服务端失败: {}", future.cause().getLocalizedMessage());
                listener.complete(null);
            }
        });
    }

    @PostConstruct
    public void initChannel() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(serverProperties.getBoss());
        workerGroup = new NioEventLoopGroup(serverProperties.getWorker());
        stationGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        // 添加心跳处理
                        ch.pipeline().addLast(new HeartHandler(250, 0, 0));
                        // 添加出站编码器
                        ch.pipeline().addLast(new GirderEncoder());
                        // 添加入站解码器
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, Integer.MAX_VALUE, 1, 4, -3, 0, true));
                        ch.pipeline().addLast(new GirderDecoder());
                        ch.pipeline().addLast(new ServerHandler(BridgeStation.this));
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
        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (stationGroup != null) {
            stationGroup.shutdownGracefully();
        }
    }
}
