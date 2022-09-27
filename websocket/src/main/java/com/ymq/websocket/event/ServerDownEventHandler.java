package com.ymq.websocket.event;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务下线事件处理
 *
 * @author lawrence
 * @since 2021/3/23
 */
@Component
@Slf4j
public class ServerDownEventHandler implements ApplicationListener<ContextClosedEvent> {

    private static final String SOCKET_NODE = "websocket_online";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @SneakyThrows
    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        log.debug("当前 WebSocket 实例 - 准备终止，即将发布下线消息. {}", contextClosedEvent);
        try {
            // Sleep 是为了确保该实例 100% 准备好了
            Thread.sleep(5000);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
        log.info("WebSocket 实例通过 Redis 发布服务上线消息：通知网关更新哈希环");
        // 这里消息内容复用面向客户端的实体类
        InetAddress address =InetAddress.getLocalHost();
        Map<String,String> map = new HashMap<>();
        String hostAddress = address.getHostAddress();
        map.put("ip",hostAddress);
        map.put("port","1207");
        map.put("online","0");
        redisTemplate.convertAndSend(SOCKET_NODE, JSON.toJSONString(map));
    }
}
