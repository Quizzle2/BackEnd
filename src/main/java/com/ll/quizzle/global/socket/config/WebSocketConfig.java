package com.ll.quizzle.global.socket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import com.ll.quizzle.global.socket.interceptor.StompChannelInterceptor;
import com.ll.quizzle.global.socket.interceptor.WebSocketHandshakeInterceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.websocket.endpoint:/ws}")
    private String endpoint;
    
    @Value("${spring.websocket.allowed-origins:*}")
    private String[] allowedOrigins;
    
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final StompChannelInterceptor channelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        log.debug("WebSocket 메시지 브로커 설정");
        
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(2); // 하트비트용 1개, 토큰 검증용 1개
        taskScheduler.setThreadNamePrefix("ws-scheduler-");
        taskScheduler.initialize();

        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(taskScheduler)
                .setHeartbeatValue(new long[]{25000, 25000});
        log.debug("메시지 브로커 활성화: /topic, /queue");

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        
        log.debug("WebSocket 메시지 브로커 설정 완료");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.debug("WebSocket 엔드포인트 등록: {}", endpoint);
        
        registry.addEndpoint(endpoint)
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS()
                .setWebSocketEnabled(true)
                .setSessionCookieNeeded(false)
                .setDisconnectDelay(30 * 1000)
                .setHeartbeatTime(25000)
                .setInterceptors(handshakeInterceptor);
        
        log.debug("WebSocket 엔드포인트 등록 완료");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(channelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setSendTimeLimit(30 * 1000)
                   .setSendBufferSizeLimit(512 * 1024)
                   .setMessageSizeLimit(128 * 1024);
    }
}