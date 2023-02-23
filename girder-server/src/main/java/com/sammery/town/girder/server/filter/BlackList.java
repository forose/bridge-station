package com.sammery.town.girder.server.filter;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;

import java.net.InetSocketAddress;

/**
 * 黑名单过滤
 * @author 沙漠渔
 */
public class BlackList implements IpFilterRule {
    @Override
    public boolean matches(InetSocketAddress inetSocketAddress) {
        return false;
    }

    @Override
    public IpFilterRuleType ruleType() {
        return IpFilterRuleType.REJECT;
    }
}
