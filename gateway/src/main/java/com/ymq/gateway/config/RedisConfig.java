package com.ymq.gateway.config;

import com.ymq.gateway.constant.GlobalConstant;
import com.ymq.gateway.redis.WebsocketSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis pub/sub 配置
 *
 * @author lawrence
 * @since 2021/3/23
 */
@Configuration
public class RedisConfig {

    @Autowired
    private WebsocketSubscriber websocketSubscriber;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        //将消息侦听器添加到（可能正在运行的）容器中。 如果容器正在运行，则侦听器会尽快开始接收（匹配）消息。
        // a 订阅了 topica、topicb 两个 频道
        container.addMessageListener(websocketSubscriber,new PatternTopic(GlobalConstant.WEBSOCKET_ONLINE));
        return container;
    }
}
