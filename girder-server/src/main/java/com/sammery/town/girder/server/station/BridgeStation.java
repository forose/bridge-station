package com.sammery.town.girder.server.station;

import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.consts.MessageType;
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
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
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

    private Channel channel;

    private final Map<String, Channel> binding = new ConcurrentHashMap<>();

    public synchronized void bind(Channel channel, boolean station) {
        String key = channel.attr(Constants.CHANNEL_KEY).get();
        Channel has = binding.get(key);
        if (has != null){
            has.attr(Constants.NEXT_CHANNEL).set(channel);
            channel.attr(Constants.NEXT_CHANNEL).set(has);
            GirderMessage message = new GirderMessage();
            message.setType(MessageType.CONNECT);
            message.setData(key.getBytes());
            if (station){
                has.writeAndFlush(message);
            }else {
                channel.writeAndFlush(message);
            }
        }else {
            binding.put(key,channel);
        }
    }

    public void link(String ip, int port, final ChannelListener listener) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        new Bootstrap().group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY,true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addLast(new StationDecoder());
                        channel.pipeline().addLast(new StationEncoder());
                        channel.pipeline().addLast(new StationHandler(BridgeStation.this));
                    }
                }).connect("127.0.0.1", 28089).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("连接服务端成功: {}", future.channel());
                listener.complete(future.channel());
            } else {
                log.warn("connect proxy server failed", future.cause());
                listener.complete(null);
            }
        }).sync();
    }

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
                        ch.pipeline().addLast(new HeartHandler(30, 0, 0));
                        // 添加出站编码器
                        ch.pipeline().addLast(new GirderEncoder());
                        // 添加入站解码器
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, 1024 * 8, 1, 2, -1, 0, true));
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
    }
}
