package com.sammery.town.girder.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 入口
 *
 * @author 沙漠渔
 */
@SpringBootApplication
@Slf4j
public class ServerApplication {

    public static void main(String[] args) {
        // 去掉banner信息
        SpringApplication app = new SpringApplication(ServerApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}
