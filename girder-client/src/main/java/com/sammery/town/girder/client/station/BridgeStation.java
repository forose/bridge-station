package com.sammery.town.girder.client.station;

import com.sammery.town.girder.client.handler.ClientHandler;
import com.sammery.town.girder.client.handler.HeartHandler;
import com.sammery.town.girder.client.handler.StationHandler;
import com.sammery.town.girder.common.listener.ChannelListener;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.consts.Command;
import com.sammery.town.girder.common.domain.GirderMessage;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static com.sammery.town.girder.common.consts.Command.CONNECT;

/**
 * 塔台
 *
 * @author 沙漠渔
 */
@Slf4j
@Service
public class BridgeStation {

    /**
     * 负责连接请求
     */
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    /**
     * 负责事件响应
     */
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    /**
     * 桥头堡Server端
     */
    private final ServerBootstrap stationBootstrap = new ServerBootstrap();

    private final Bootstrap transferBootstrap = new Bootstrap();

    private static final ConcurrentLinkedQueue<Channel> CHANNEL_POOL = new ConcurrentLinkedQueue<>();

    private static final int MAX_POOL_SIZE = 10;

    private Channel manageChannel;

    private Map<Integer, Channel> stations = new ConcurrentHashMap<>();

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

    /**
     * 申请与服务端建立连接 通过控制通道传递连接信息，实现鉴权、控制，实现服务端与对端同步建立，减少时间消耗
     *
     * @param ctx 上下文
     */
    public void active(ChannelHandlerContext ctx) {
        // 发送连接请求给服务端 做好连接准备
        GirderMessage message = new GirderMessage();
        message.setCmd(CONNECT);
        String addr = ctx.channel().localAddress().toString().substring(1);

        message.setData((ctx.channel().id().asShortText() + "@" + addr).getBytes());
        manageChannel.writeAndFlush(message);
    }

    /**
     * 获取客户端与终端之间的连接
     *
     * @param listener 成功之后的回调
     * @throws Exception
     */
    public void borrowChannel(final ChannelListener listener) throws Exception {
        Channel channel = poll();
        if (channel != null) {
            listener.complete(channel);
        } else {
            transferBootstrap.connect("127.0.0.1", 39001).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("通道连接成功: {}", future.channel());
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
            }
        }
        return null;
    }

    public synchronized void returnChanel(Channel channel) {
        channel.attr(Constants.STATUS_RETURN).set(false);
        if (CHANNEL_POOL.size() > MAX_POOL_SIZE) {
            channel.close();
        } else {
            channel.config().setAutoRead(true);
            channel.attr(Constants.NEXT_CHANNEL).set(null);
            CHANNEL_POOL.offer(channel);
            log.debug("归还通道连接: {}, 当前连接池大小: {} ", channel, CHANNEL_POOL.size());
        }
    }

    @PostConstruct
    public void init() {
        stationBootstrap.group(bossGroup, workerGroup);
        stationBootstrap.channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true);
        stationBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new StationDecoder());
                ch.pipeline().addLast(new StationEncoder());
                // 添加处理器
                ch.pipeline().addLast(new StationHandler(BridgeStation.this));
            }
        });

        transferBootstrap.group(workerGroup).channel(NioSocketChannel.class);
        transferBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                // 添加心跳处理
                ch.pipeline().addLast(new HeartHandler(0, 120, 0));
                // 添加出站编码器
                ch.pipeline().addLast(new GirderEncoder());
                // 添加入站编码器
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, 1024 * 1024, 1, 2, -1, 0, true));
                ch.pipeline().addLast(new GirderDecoder());
                ch.pipeline().addLast(new ClientHandler(BridgeStation.this));
            }
        });
        link();
    }

    public synchronized void link() {
        for (Channel channel : stations.values()) {
            if (channel.isActive()) {
                channel.close();
            }
        }
        stations.clear();
        if (manageChannel == null || !manageChannel.isOpen() || !manageChannel.isActive()) {
            transferBootstrap.connect("127.0.0.1", 39001).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    GirderMessage message = new GirderMessage();
                    message.setCmd(Command.AUTH);
                    message.setData(new byte[]{0x11});
                    future.channel().writeAndFlush(message);
                    future.channel().attr(Constants.MANAGE_CHANNEL).set(true);
                    manageChannel = future.channel();
                    log.info("当前控制链路: " + manageChannel);
                } else {
                    TimeUnit.SECONDS.sleep(10);
                    link();
                }
            });
        }
    }

    @PreDestroy
    public void destroy() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
