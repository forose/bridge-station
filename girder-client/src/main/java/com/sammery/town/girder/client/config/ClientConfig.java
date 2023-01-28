package com.sammery.town.girder.client.config;

import com.sammery.town.girder.client.properties.ClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 装配
 *
 * @author 沙漠渔
 */
@Configuration
@EnableConfigurationProperties(ClientProperties.class)
public class ClientConfig {
    @Bean
    @ConditionalOnMissingBean
    public ClientProperties clientProperties() {
        return new ClientProperties();
    }
}
