package com.sammery.town.girder.server.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 沙漠渔
 */
@Getter@Setter
@ConfigurationProperties(prefix = "girder.server")
public class ServerProperties {
    private Integer boss = 2;

    private Integer worker = 2;

    private Integer port = 9001;
}
