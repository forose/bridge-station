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
    /**
     * 服务端IP
     */
    private String host = "127.0.0.1";
    /**
     * 服务端端口
     */
    private Integer port = 39001;

    /**
     * 唯一性标识
     */
    private String identity;

    /**
     * 默认的虚拟网卡名称 用于自动添加内网IP
     */
    private String net="VirtualBox Host-Only Network";

}
