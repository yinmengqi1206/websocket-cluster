package com.ymq.gateway.config;

import com.ymq.gateway.filter.CustomReactiveLoadBalanceFilter;
import com.ymq.gateway.hashring.ConsistentHashRouter;
import com.ymq.gateway.hashring.ServiceNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 初始化
 *
 * @author lawrence
 * @since 2021/3/23
 */
@Configuration
@Slf4j
public class GatewayHashRingConfig {
    private final LoadBalancerClientFactory clientFactory;

    public GatewayHashRingConfig(LoadBalancerClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /**
     * @param properties 负载均衡配置
     * @param consistentHashRouter   {@link #init() init方法}注入，此处未使用构造注入（会产生循环依赖）
     * @return 注入自定义的 Reactive 过滤器 Bean 对象
     */
    @Bean
    public CustomReactiveLoadBalanceFilter customReactiveLoadBalanceFilter(GatewayLoadBalancerProperties properties,
                                                                           ConsistentHashRouter<ServiceNode> consistentHashRouter) {
        return new CustomReactiveLoadBalanceFilter(clientFactory, properties, consistentHashRouter);
    }

    @Bean
    public ConsistentHashRouter<ServiceNode> init() {
        //初始化6个真实节点和各自100个虚拟节点
        // 获取环中的所有真实节点
        List<ServiceNode> serviceNodes = new ArrayList<>();
        //TODO redis查询，发布订阅节点变化
        for (int i = 1; i < 7; i++) {
            ServiceNode physicalNode = new ServiceNode("127.0.0.1",1200+i);
            serviceNodes.add(physicalNode);
        }
        return new ConsistentHashRouter<>(serviceNodes,100);
    }

}
