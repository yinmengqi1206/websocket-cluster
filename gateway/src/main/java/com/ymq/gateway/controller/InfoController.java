package com.ymq.gateway.controller;

import com.ymq.gateway.constant.GlobalConstant;
import com.ymq.gateway.hashring.ServiceNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yinmengqi
 * @version 1.0
 * @date 2023/6/29 15:32
 */
@RestController
@Slf4j
@RequestMapping("info")
public class InfoController {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @GetMapping("/nodes")
    public List<ServiceNode> nodes(){
        // 先从 Redis 中获取哈希环(网关集群)
        final Map<Object,Object> ring = redisTemplate.opsForHash().entries(GlobalConstant.HASH_RING_REDIS);
        // 获取环中的所有真实节点
        return ring.values().stream().map(x -> (ServiceNode) x).collect(Collectors.toList());
    }

    @SneakyThrows
    @GetMapping("/ip")
    public String ip(){
        InetAddress localhost = InetAddress.getLocalHost();
        String ip = localhost.getHostAddress();
        System.out.println("本机IP地址：" + ip);
        return ip;
    }
}
