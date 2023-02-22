package com.sammery.town.girder.client.station;

import com.sammery.town.girder.client.handler.ClientHandler;
import com.sammery.town.girder.client.handler.HeartHandler;
import com.sammery.town.girder.client.handler.StationHandler;
import com.sammery.town.girder.client.properties.ClientProperties;
import com.sammery.town.girder.common.consts.Command;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.listener.ChannelListener;
import com.sammery.town.girder.common.protocol.GirderDecoder;
import com.sammery.town.girder.common.protocol.GirderEncoder;
import com.sammery.town.girder.common.protocol.StationDecoder;
import com.sammery.town.girder.common.protocol.StationEncoder;
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
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * 塔台桥头堡
 *
 * @author 沙漠渔
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BridgeStation {

    /**
     * 用于执行本地脚本使用
     */
    public static Runtime RUNTIME = Runtime.getRuntime();

    private final ClientProperties clientProperties;

    /**
     * 负责连接请求
     */
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    /**
     * 负责事件响应
     */
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    /**
     * 桥头堡Server端 支持地址复用
     */
    private final ServerBootstrap stationBootstrap = new ServerBootstrap().childOption(ChannelOption.SO_REUSEADDR, true);

    private final Bootstrap transferBootstrap = new Bootstrap();

    private static final ConcurrentLinkedQueue<Channel> CHANNEL_POOL = new ConcurrentLinkedQueue<>();

    private static final int MAX_POOL_SIZE = 50;

    private Channel manageChannel;

    private final Map<Integer, Channel> stations = new ConcurrentHashMap<>();

    private final Map<String, String> networks = new ConcurrentHashMap<>();

    /**
     * 打开本地需要启动的服务端口
     *
     * @param port 服务端在鉴权通过时给出的服务端口
     */
    public void open(Integer port) {
        ChannelFuture channelFuture = stationBootstrap.bind(port);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                 stations.put(port, future.channel());
                log.info("BridgeStation success bind to " + port);
            } else {
                log.error("BridgeStation fail bind to " + port);
            }
        });
    }

    public void network(String ip) {
        try {
            int res = RUNTIME.exec("cmd /c " + "netsh interface ipv4 add address \"" + clientProperties.getNet() + "\" " + ip + " 255.255.255.0").waitFor();
            if (res == 0){
                networks.put(ip, ip);
            }
            log.info("IP资源添加成功");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            log.info("IP资源添加失败");
            networks.remove(ip);
        }
    }

    private void release() {
        for (Channel channel : stations.values()) {
            if (channel.isActive()) {
                channel.close();
            }
        }
        stations.clear();
        for (String ip : networks.values()) {
            try {
                RUNTIME.exec("cmd /c " + "netsh interface ipv4 delete address \"" + clientProperties.getNet() + "\" " + ip + " 255.255.255.0");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        networks.clear();
    }

    /**
     * 申请与服务端建立连接 通过控制通道传递连接信息，实现鉴权、控制，实现服务端与对端同步建立，减少时间消耗
     */
    public void active(GirderMessage message) {
        manageChannel.writeAndFlush(message);
    }

    /**
     * 获取客户端与终端之间的连接
     *
     * @param listener 成功之后的回调
     */
    public void borrowChannel(final ChannelListener listener) {
        Channel channel = poll();
        if (channel != null) {
            listener.complete(channel);
        } else {
            transferBootstrap.connect().addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("通道连接成功: {}", future.channel());
                    future.channel().attr(Constants.MANAGE_CHANNEL).set(false);
                    listener.complete(future.channel());
                } else {
                    log.warn("通道连接失败", future.cause());
                    listener.complete(null);
                }
            });
        }
    }

    private Channel poll() {
        while (!CHANNEL_POOL.isEmpty()) {
            Channel channel = CHANNEL_POOL.poll();
            if (channel == null) {
                return null;
            } else if (channel.isActive() && channel.isOpen()) {
                return channel;
            } else {
                channel.close();
            }
        }
        return null;
    }

    public synchronized void returnChanel(Channel channel) {
        channel.attr(Constants.STATUS_RETURN).set(false);
        if (CHANNEL_POOL.size() >= MAX_POOL_SIZE) {
            channel.close();
        } else {
            if (channel.isActive() && channel.isOpen()) {
                channel.attr(Constants.NEXT_CHANNEL).set(null);
                CHANNEL_POOL.offer(channel);
                log.debug("归还通道连接: {}, 当前连接池: {} ", channel, CHANNEL_POOL.size());
            } else {
                log.debug("通道连接已断开,丢弃: " + channel);
            }
        }
    }

    @PostConstruct
    public void initStation() {
        stationBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new StationDecoder());
                        ch.pipeline().addLast(new StationEncoder());
                        // 添加处理器
                        ch.pipeline().addLast(new StationHandler(BridgeStation.this));
                    }
                });

        transferBootstrap.group(workerGroup)
                .channel(NioSocketChannel.class).remoteAddress(clientProperties.getHost(), clientProperties.getPort())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        // 添加心跳处理
                        ch.pipeline().addLast(new HeartHandler(0, 120, 0));
                        // 添加出站编码器
                        ch.pipeline().addLast(new GirderEncoder());
                        // 添加入站编码器
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, Integer.MAX_VALUE, 1, 4, -3, 0, true));
                        ch.pipeline().addLast(new GirderDecoder());
                        ch.pipeline().addLast(new ClientHandler(BridgeStation.this));
                    }
                });
        link();
    }

    public synchronized void link() {
        release();
        if (manageChannel == null || !manageChannel.isOpen() || !manageChannel.isActive()) {
            transferBootstrap.connect().addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    GirderMessage message = new GirderMessage();
                    message.setCmd(Command.AUTH);
                    // todo 待确认是通过什么方式进行验证
                    message.setData(new byte[]{0x11});
                    future.channel().writeAndFlush(message);
                    future.channel().attr(Constants.MANAGE_CHANNEL).set(true);
                    manageChannel = future.channel();
                    log.info("当前管理通道: " + manageChannel);
                } else {
                    TimeUnit.SECONDS.sleep(10);
                    link();
                }
            });
        }
    }

    @PreDestroy
    public void destroyStation() {
        release();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
