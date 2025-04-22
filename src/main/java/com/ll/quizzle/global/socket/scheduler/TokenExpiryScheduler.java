package com.ll.quizzle.global.socket.scheduler;

import com.ll.quizzle.global.socket.service.WebSocketNotificationService;
import com.ll.quizzle.global.socket.session.WebSocketSessionManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class TokenExpiryScheduler {

    @Value("${custom.jwt.token.expiry-checking-seconds}")
    private long tokenExpiryCheckingSeconds;

    private final WebSocketSessionManager sessionManager;
    private final WebSocketNotificationService notificationService;
    
    private ThreadPoolTaskScheduler taskScheduler;

    @PostConstruct
    public void init() {
        startTokenExpiryChecker();
    }


    private void startTokenExpiryChecker() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-token-checker-");
        taskScheduler.initialize();
        
        taskScheduler.scheduleAtFixedRate(() -> {
            log.debug("토큰 만료 검사 실행 중...");
            
            long currentTime = System.currentTimeMillis();
            
            sessionManager.removeExpiredSessions(currentTime, 
                (email, sessionInfo) -> notificationService.sendTokenExpiredNotification(email));
            
        }, tokenExpiryCheckingSeconds * 1000);
    }
} 