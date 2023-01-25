package com.sammery.town.girder.common.listener;

import io.netty.channel.Channel;

import java.util.EventListener;

/**
 * 连接请求的回调
 * @author 沙漠渔
 */
public interface ChannelListener extends EventListener {
    /**
     * 返回结果 的回调 channel存在为null的情况,如果为null 表示未获取到
     * @param channel 连接
     * @throws Exception 抛出异常
     */
    void complete(Channel channel);
}