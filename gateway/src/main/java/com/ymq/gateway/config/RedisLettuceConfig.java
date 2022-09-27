package com.ymq.gateway.config;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @ClassName: RedisLettuceConfig
 * @Description: spring2.x的lettuce配置 代替jedis的配置
 * 定义序列化为fastjson
 * @Author: yanglei
 * @Date: 2018-12-08 15:12
 * @Version: 1.0
 */
@Configuration
@Slf4j
@Data
public class RedisLettuceConfig {


    @Bean(name = "stringRedisTemplate")
    @Primary
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        log.info("初始化 stringRedisTemplate");
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);
        log.info("初始化 stringRedisTemplate end");
        return stringRedisTemplate;
    }

    /**
     * 自定义redisTemplate
     * springboot2.x 使用LettuceConnectionFactory 代替 RedisConnectionFactory
     * 在application.yml配置基本信息后,springboot2.x
     * RedisAutoConfiguration能够自动装配 LettuceConnectionFactory
     * 和 RedisConnectionFactory 及其 RedisTemplate
     *
     * @param redisConnectionFactory
     * @return
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        log.info("初始化 redisTemplate");
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //redis开启事务
        //redisTemplate.setEnableTransactionSupport(true);
        //配置序列化方式
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //使用fastjson 代替 GenericJackson2JsonRedisSerializer
        redisTemplate.setValueSerializer(new GenericFastJsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericFastJsonRedisSerializer());
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        log.info("初始化 redisTemplate end");
        return redisTemplate;
    }
}