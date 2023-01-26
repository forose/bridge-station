package com.sammery.town.girder.common.consts;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * @author forose
 */
public interface Constants {

    /**
     * 连接的下一跳连接
     */
    AttributeKey<Channel> NEXT_CHANNEL = AttributeKey.newInstance("next_channel");

    /**
     * 是否为管理用的通道连接 用于做特殊处理
     */
    AttributeKey<Boolean> MANAGE_CHANNEL = AttributeKey.newInstance("manage_channel");

    /**
     * 通道连接的key 用于服务端进行连接绑定使用 (异步连接,通道连接和实际连接存有相同的key 都是按照客户端那边的id@lan)
     */
    AttributeKey<String> CHANNEL_KEY = AttributeKey.newInstance("channel_key");

    /**
     * 通道连接待归还状态标志
     */
    AttributeKey<Boolean> STATUS_RETURN = AttributeKey.newInstance("status_return");
}
