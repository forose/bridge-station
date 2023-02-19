package com.sammery.town.girder.client;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 客户端入口
 *
 * @author 沙漠渔
 */
@SpringBootApplication
public class ClientApplication {
    public static void main(String[] args) {
        // 去掉banner信息
        SpringApplication app = new SpringApplication(ClientApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

}
