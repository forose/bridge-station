package com.sammery.town.girder.server.config;

import com.sammery.town.girder.server.properties.ServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 装配
 * @author 沙漠渔
 */
@Configuration
@EnableConfigurationProperties(ServerProperties.class)
public class ServerConfig {
    @Bean
    @ConditionalOnMissingBean
    public ServerProperties serverProperties(){
        return new ServerProperties();
    }
}
