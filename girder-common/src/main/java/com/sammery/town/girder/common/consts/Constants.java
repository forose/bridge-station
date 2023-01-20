package com.sammery.town.girder.common.consts;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * @author forose
 */
public interface Constants {

    AttributeKey<Channel> NEXT_CHANNEL = AttributeKey.newInstance("next_channel");

    AttributeKey<Boolean> MANAGE_CHANNEL = AttributeKey.newInstance("manage_channel");

    AttributeKey<String> CHANNEL_KEY = AttributeKey.newInstance("channel_key");
}
