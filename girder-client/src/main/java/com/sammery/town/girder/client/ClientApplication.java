package com.sammery.town.girder.client;

import com.sammery.town.girder.client.handler.ClientHandler;
import com.sammery.town.girder.client.handler.HeartHandler;
import com.sammery.town.girder.client.station.BridgeStation;
import com.sammery.town.girder.common.consts.Constants;
import com.sammery.town.girder.common.consts.MessageType;
import com.sammery.town.girder.common.domain.GirderMessage;
import com.sammery.town.girder.common.protocol.GirderDecoder;
import com.sammery.town.girder.common.protocol.GirderEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 客户端入口
 * @author 沙漠渔
 */
@SpringBootApplication
public class ClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

}
