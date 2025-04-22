package com.ll.quizzle.global.socket.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 웹 소켓 세션 관리 전략 팩토리 (추후 확장성을 고려)
 * 예로들어 새로운 이벤트 세션 즉, 카프카, RabbitMQ 등으로 대체할 경우를 대비
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionRegistry {

    private final WebSocketSessionManager inMemorySessionManager;
    private final WebSocketSessionManager redisSessionManager;


    @Value("${app.websocket.use-distributed-sessions:true}")
    private boolean useDistributedSessions;
    

    public WebSocketSessionManager getSessionManager() {
        if (useDistributedSessions) {
            log.debug("Redis 기반 세션 매니저 사용");
            return redisSessionManager;
        } else {
            log.debug("인메모리 기반 세션 매니저 사용");
            return inMemorySessionManager;
        }
    }
} 