package com.ymq.gateway.redis;

import com.alibaba.fastjson.JSON;
import com.ymq.gateway.constant.GlobalConstant;
import com.ymq.gateway.hashring.ConsistentHashRouter;
import com.ymq.gateway.hashring.ServiceNode;
import com.ymq.gateway.hashring.VirtualNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author yinmengqi
 * @version 1.0
 * @date 2022/9/27 16:25
 */
@Component
@Slf4j
public class WebsocketSubscriber implements MessageListener {

    @Autowired
    private ConsistentHashRouter<ServiceNode> serviceNodeConsistentHashRouter;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String strMessage = redisTemplate.getValueSerializer().deserialize(message.getBody()).toString();
        HashMap<String,String> hashMap = JSON.parseObject(strMessage, HashMap.class);
        if(Objects.equals(hashMap.get("online"), GlobalConstant.SERVER_UP_MESSAGE)){
            log.info("服务上线 message:{}",hashMap);
            ServiceNode serviceNode = new ServiceNode(hashMap.get("ip"),Integer.parseInt(hashMap.get("port")));
            serviceNodeConsistentHashRouter.addNode(serviceNode,100);
            redisTemplate.opsForHash().put(GlobalConstant.HASH_RING_REDIS,serviceNode.getKey(),serviceNode);
        }
        if(Objects.equals(hashMap.get("online"),GlobalConstant.SERVER_DOWN_MESSAGE)){
            log.info("服务下线 message:{}",hashMap);
            ServiceNode serviceNode = new ServiceNode(hashMap.get("ip"),Integer.parseInt(hashMap.get("port")));
            serviceNodeConsistentHashRouter.removeNode(serviceNode);
            redisTemplate.opsForHash().delete(GlobalConstant.HASH_RING_REDIS,serviceNode.getKey());
        }
    }
}
