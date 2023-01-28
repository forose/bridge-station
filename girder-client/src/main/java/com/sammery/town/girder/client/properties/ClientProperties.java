package com.sammery.town.girder.client.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 沙漠渔
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "girder.client")
public class ClientProperties {
    private String host = "127.0.0.1";

    private Integer port = 39001;
}
