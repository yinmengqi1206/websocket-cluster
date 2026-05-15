package com.ymq.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.yeauty.annotation.EnableWebSocket;

/**
 * @author yinmengqi
 * @version 1.0
 * @date 2022/9/26 15:37
 */
@SpringBootApplication
@EnableWebSocket
public class WebSocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSocketApplication.class);
    }
}
