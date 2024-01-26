package com.ymq.websocket.event;

import com.alibaba.fastjson.JSON;
import com.ymq.websocket.server.WebSocketConst;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务上线事件处理
 *
 * @author lawrence
 * @since 2021/3/21
 */
@Component
@Slf4j
public class ServerUpEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    private static final String SOCKET_NODE = "websocket_online";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @SneakyThrows
    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.debug("当前 WebSocket 实例 - 准备就绪，即将发布上线消息. {}", applicationReadyEvent);
        // 这里消息内容复用面向客户端的实体类
        InetAddress address =InetAddress.getLocalHost();
        Map<String,String> map = new HashMap<>();
        String hostAddress = address.getHostAddress();
        map.put("ip",hostAddress);
        map.put("port", WebSocketConst.PORT);
        map.put("online","1");
        redisTemplate.convertAndSend(SOCKET_NODE, JSON.toJSONString(map));
    }
}
