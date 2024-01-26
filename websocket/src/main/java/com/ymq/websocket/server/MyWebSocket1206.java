package com.ymq.websocket.server;

import com.ymq.websocket.util.SocketUtil;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yeauty.annotation.*;
import org.yeauty.pojo.Session;

import java.io.IOException;
import java.util.Objects;

import static com.ymq.websocket.server.WebSocketConst.PORT;

/**
 * @author yinmengqi
 * @version 1.0
 * @date 2022/9/22 14:30
 */
@ServerEndpoint(path = "/websocket/{userId}",port = PORT)
@Component
@Slf4j
public class MyWebSocket1206 {

    private Long getUserId(Session session){
        return session.getAttribute("userId");
    }

    @BeforeHandshake
    public void handshake(Session session, HttpHeaders headers, @PathVariable Long userId){
        session.setSubprotocols("stomp");
    }

    @OnOpen
    public void onOpen(Session session, HttpHeaders headers, @PathVariable Long userId){
        if(Objects.isNull(userId)){
            log.error("用户id{}不存在",userId);
            session.close();
            return;
        }
        SocketUtil.putSession(userId,session);
        session.setAttribute("userId",userId);
        log.info("用户 {}new connection",userId);
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        Long userId = getUserId(session);
        SocketUtil.delSession(userId);
        log.info("{}断开socket连接",userId);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        Long userId = getUserId(session);
        log.info("{}socket异常",userId);
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        Long userId = getUserId(session);
        log.info("{}发送消息:{}",userId,message);
    }

    @OnBinary
    public void onBinary(Session session, byte[] bytes) {
        Long userId = getUserId(session);
        log.info("{}发送字节消息:{}",userId,new String(bytes));
    }

}
