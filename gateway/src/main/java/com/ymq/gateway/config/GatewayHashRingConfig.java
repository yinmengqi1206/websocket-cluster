package com.ymq.gateway.config;

import com.ymq.gateway.constant.GlobalConstant;
import com.ymq.gateway.filter.WebSocketLoadBalanceFilter;
import com.ymq.gateway.hashring.ConsistentHashRouter;
import com.ymq.gateway.hashring.ServiceNode;
import com.ymq.gateway.hashring.VirtualNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 初始化哈希环
 * @author yinmengqi
 * @version 1.0
 * @date 2022/9/26 15:37
 */
@Configuration
@Slf4j
public class GatewayHashRingConfig {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private LoadBalancerClientFactory clientFactory;

    /**
     * @param properties 负载均衡配置
     * @param consistentHashRouter   {@link #init() init方法}注入，此处未使用构造注入（会产生循环依赖）
     * @return 注入自定义的 Reactive 过滤器 Bean 对象
     */
    @Bean
    public WebSocketLoadBalanceFilter customReactiveLoadBalanceFilter(GatewayLoadBalancerProperties properties,
                                                                      ConsistentHashRouter<ServiceNode> consistentHashRouter) {
        return new WebSocketLoadBalanceFilter(clientFactory, properties, consistentHashRouter);
    }

    @Bean
    public ConsistentHashRouter<ServiceNode> init() {
        // 先从 Redis 中获取哈希环(网关集群)
        final Map<Object,Object> ring = redisTemplate.opsForHash().entries(GlobalConstant.HASH_RING_REDIS);
        // 获取环中的所有真实节点
        List<ServiceNode> serviceNodes = ring.values().stream().map(x -> (ServiceNode) x).collect(Collectors.toList());
        ConsistentHashRouter<ServiceNode> consistentHashRouter = new ConsistentHashRouter<>(serviceNodes, GlobalConstant.VIRTUAL_COUNT);
        log.info("初始化 ConsistentHashRouter: {}", serviceNodes);
        return consistentHashRouter;
    }

}
