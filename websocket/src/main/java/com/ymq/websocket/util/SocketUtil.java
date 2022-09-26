package com.ymq.websocket.util;

import org.yeauty.pojo.Session;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yinmengqi
 * @version 1.0
 * @date 2022/9/23 10:25
 */
public class SocketUtil {

    /**
     * session Map
     */
    private static final ConcurrentHashMap<Long, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 断开删除
     * @param userId
     */
    public static void delSession(Long userId){
        SESSION_MAP.remove(userId);
    }

    /**
     * 内存保存session
     * @param userId
     * @param session
     */
    public static void putSession(Long userId,Session session){
        //如果已存在，关闭连接
        if(SESSION_MAP.containsKey(userId)){
            SESSION_MAP.get(userId).close();
        }
        SESSION_MAP.put(userId,session);
    }

    /**
     * 发送json数据到用户
     * @param userId
     * @param messageVo
     */
    public static void sendToUser(Long userId, String messageVo){
        if(!SESSION_MAP.containsKey(userId)){
            return;
        }
        Session session = SESSION_MAP.get(userId);
        session.sendText(messageVo);
    }
}
