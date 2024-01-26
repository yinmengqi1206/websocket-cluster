package com.ymq.gateway.filter;

import com.ymq.gateway.hashring.ConsistentHashRouter;
import com.ymq.gateway.hashring.ServiceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * 响应式网关负载均衡过滤器
 * @author yinmengqi
 * @version 1.0
 * @date 2022/9/26 15:37
 */
public class WebSocketLoadBalanceFilter extends ReactiveLoadBalancerClientFilter implements BeanPostProcessor {

    private static final String ROUTE_ID = "websocket-route";

    private static final Logger logger = LoggerFactory.getLogger(WebSocketLoadBalanceFilter.class);

    final LoadBalancerClientFactory clientFactory;
    final GatewayLoadBalancerProperties properties;
    final ConsistentHashRouter<ServiceNode> consistentHashRouter;

    public WebSocketLoadBalanceFilter(LoadBalancerClientFactory clientFactory,
                                      GatewayLoadBalancerProperties properties,
                                      ConsistentHashRouter<ServiceNode> consistentHashRouter) {
        super(clientFactory, properties);
        this.clientFactory = clientFactory;
        this.properties = properties;
        this.consistentHashRouter = consistentHashRouter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        //仅支持websocket-route
        boolean support = Objects.nonNull(route) && !Objects.equals(route.getId(), ROUTE_ID);
        if (Objects.isNull(url)|| support) {
            return chain.filter(exchange);
        }
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        //添加原始请求地址
        addOriginalRequestUrl(exchange, url);
        return chooseInstance(exchange).doOnNext(response -> {
            URI uri = exchange.getRequest().getURI();
            //构建DefaultServiceInstance
            DefaultServiceInstance defaultServiceInstance = new DefaultServiceInstance(UUID.randomUUID().toString(),ROUTE_ID,response.getKey(),response.getPort(),false);
            String overrideScheme = defaultServiceInstance.isSecure() ? "wss" : "ws";
            if (schemePrefix != null) {
                overrideScheme = url.getScheme();
            }
            DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance(defaultServiceInstance,overrideScheme);
            //重构url
            URI requestUrl = reconstructURI(serviceInstance, uri);
            logger.info("【网关负载均衡】连接socket {}",requestUrl);
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
        }).then(chain.filter(exchange));
    }



    /**
     * 选择实例节点
     * @param exchange
     * @return
     */
    private Mono<ServiceNode> chooseInstance(ServerWebExchange exchange) {
        URI uri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        assert uri != null;
        String userIdFromRequest = getUserIdFromRequest(exchange);
        ServiceNode serviceNode = consistentHashRouter.routeNode(userIdFromRequest);
        if(Objects.isNull(serviceNode)){
            logger.error("无可用节点");
            return Mono.empty();
        }
        return Mono.just(serviceNode);
    }

    /**
     * 从 WS/HTTP 请求 中获取待哈希字段 userId
     *
     * @param exchange 请求上下文
     * @return userId，可能为空
     */
    protected static String getUserIdFromRequest(ServerWebExchange exchange) {
        String userId = null;
        // ws: "lb://websocket-server/1" 获取这里面的最后一个路径参数 userId: 1
        List<PathContainer.Element> elements = exchange.getRequest().getPath().elements();
        PathContainer.Element lastElement = elements.get(elements.size() - 1);
        userId = lastElement.value();
        logger.debug("【网关负载均衡】WebSocket 获取到 userId: {}", userId);
        return userId;
    }

}
