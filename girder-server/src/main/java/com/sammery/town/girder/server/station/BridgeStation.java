package com.sammery.town.girder.server.station;

import com.sammery.town.girder.common.consts.Command;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.listener.ChannelListener;
import com.sammery.town.girder.common.protocol.GirderDecoder;
import com.sammery.town.girder.common.protocol.GirderEncoder;
import com.sammery.town.girder.common.protocol.StationDecoder;
import com.sammery.town.girder.common.protocol.StationEncoder;
import com.sammery.town.girder.server.handler.HeartHandler;
import com.sammery.town.girder.server.handler.ServerHandler;
import com.sammery.town.girder.server.handler.StationHandler;
import com.sammery.town.girder.server.model.AccessEntity;
import com.sammery.town.girder.server.model.PersonEntity;
import com.sammery.town.girder.server.model.RelationEntity;
import com.sammery.town.girder.server.model.ServiceEntity;
import com.sammery.town.girder.server.properties.ServerProperties;
import com.sammery.town.girder.server.repository.AccessRepository;
import com.sammery.town.girder.server.repository.PersonRepository;
import com.sammery.town.girder.server.repository.RelationRepository;
import com.sammery.town.girder.server.repository.ServiceRepository;
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
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 桥头堡
 * @author 沙漠渔
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BridgeStation {
    private final ServerProperties serverProperties;
    private final ServiceRepository serviceRepository;
    private final PersonRepository personRepository;
    private final RelationRepository relationRepository;
    private final AccessRepository accessRepository;

    private final Map<String, Date> accessCache = new ConcurrentHashMap<>();
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

    private static final Map<String, Channel> BOUNDING = new HashMap<>();
    /**
     * 处于等待连接配对的集合
     */
    private static final Map<String, Channel> BINDING = new HashMap<>();

    private void bound(String key, Channel bridge) {
        Channel has = BOUNDING.get(key);
        if (has != null) {
            if (has.hasAttr(Constants.MANAGE_CHANNEL)) {
                has.attr(Constants.SLAVE_CHANNEL).get().add(bridge);
            } else {
                bridge.attr(Constants.SLAVE_CHANNEL).get().add(has);
            }
            BOUNDING.remove(key);
        } else {
            BOUNDING.put(key, bridge);
        }
    }

    public synchronized void bind(String key, Channel bridge, Channel station) {
        bound(key, bridge);
        boolean manage = bridge.hasAttr(Constants.MANAGE_CHANNEL) && bridge.attr(Constants.MANAGE_CHANNEL).get();
        bind(key, manage ? station : bridge, manage);
    }

    private void bind(String key, Channel channel, boolean station) {
        if (BINDING.containsKey(key)) {
            Channel has = BINDING.get(key);
            if (station) {
                // 如果是管理端发过来的 has就肯定是桥
                if (channel == null) {
                    GirderMessage message = new GirderMessage();
                    message.setCmd(Command.DISCON);
                    message.setData(key.getBytes());
                    has.writeAndFlush(message);
                } else {
                    has.attr(Constants.NEXT_CHANNEL).set(channel);
                    channel.attr(Constants.NEXT_CHANNEL).set(has);
                    GirderMessage message = new GirderMessage();
                    message.setCmd(Command.CONNECT);
                    message.setData(key.getBytes());
                    has.writeAndFlush(message);
                    channel.config().setAutoRead(true);
                }
            } else {
                // 否则 has就是站 channel就是桥
                if (has == null) {
                    GirderMessage message = new GirderMessage();
                    message.setCmd(Command.DISCON);
                    message.setData(key.getBytes());
                    channel.writeAndFlush(message);
                } else {
                    has.attr(Constants.NEXT_CHANNEL).set(channel);
                    channel.attr(Constants.NEXT_CHANNEL).set(has);
                    GirderMessage message = new GirderMessage();
                    message.setCmd(Command.CONNECT);
                    message.setData(key.getBytes());
                    channel.writeAndFlush(message);
                    has.config().setAutoRead(true);
                }
            }
            // 如果已经配对成功,则移除掉原有内容
            BINDING.remove(key);
        } else {
            BINDING.put(key, channel);
        }
    }

    /**
     * 用于服务端代理与实际服务连接并异步返回连接进行处理
     *
     * @param ip       需要连接的IP
     * @param port     需要连接的端口
     * @param listener 连接建立之后的动作
     */
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
                log.info("连接服务成功: {}", future.channel());
                listener.complete(future.channel());
            } else {
                log.warn("连接服务失败: {}", future.cause().getLocalizedMessage());
                listener.complete(null);
            }
        });
    }

    /**
     * 项目启动完成初始化配置,主要包括两步:
     * 第一:创建服务端
     * 第二:创建客户端连接的Group
     *
     * @throws InterruptedException 异常
     */
    @PostConstruct
    public void initStation() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(serverProperties.getBoss());
        workerGroup = new NioEventLoopGroup(serverProperties.getWorker());
        stationGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap().childOption(ChannelOption.SO_REUSEADDR, true);
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
    public void destroyStation() {
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

    public List<ServiceEntity> obtainService(PersonEntity person) {
        if (person != null) {
            List<RelationEntity> relations = relationRepository.getAllByPerson(person.getId());
            if (relations != null) {
                List<Integer> serviceIds = relations.stream().map(RelationEntity::getService).collect(Collectors.toList());
                List<ServiceEntity> services = serviceRepository.getAllByIdIn(serviceIds);
                return services == null ? new ArrayList<>() : services;
            }
        }
        return new ArrayList<>();
    }

    public PersonEntity obtainPerson(String md5, String identity) {
        PersonEntity person = personRepository.getFirstByMd5Equals(md5);
        if (person != null) {
            if (StringUtils.isEmpty(person.getIdentity())) {
                person.setIdentity(identity);
                personRepository.save(person);
            }
            if (person.getIdentity().equals(identity)) {
                return person;
            }
        }
        return null;
    }

    public void saveAccess(AccessEntity access) {
        Date now = new Date();
        String key = access.getPerson().concat("@").concat(access.getService());
        if (accessCache.containsKey(key)) {
            Date last = accessCache.get(key);
            if (now.getTime() - last.getTime() > 1000 * 300) {
                accessCache.put(key, now);
                accessRepository.save(access);
            }
        } else {
            accessCache.put(key, now);
            accessRepository.save(access);
        }
    }
}
