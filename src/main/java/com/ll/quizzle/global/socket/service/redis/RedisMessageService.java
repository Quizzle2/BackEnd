package com.ll.quizzle.global.socket.service.redis;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import com.ll.quizzle.global.socket.core.MessageCallback;
import com.ll.quizzle.global.socket.core.MessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis PUB/SUB 기반 메시징 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageService implements MessageService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    /**
     * 여기서도 공유자원 관리를 위해 ConcurrentHashMap 을 사용합니다.
     * STOMP 와 Redis Pub/Sub 의 구독 방식이 조금 달라서 찾아보시면 도움 되실겁니다.
     */
    private final Map<String, MessageListener> listeners = new ConcurrentHashMap<>();
    private final Map<String, ChannelTopic> topics = new ConcurrentHashMap<>();
    private boolean connected = false;

    @Override
    public void send(String destination, Object message) {
        try {
            String messageStr = (message instanceof String) ? (String) message : message.toString();
            log.debug("Redis 채널로 메시지 전송: {}", destination);
            redisTemplate.convertAndSend(destination, messageStr);
        } catch (Exception e) {
            log.error("Redis 메시지 전송 실패: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendToUser(String userId, Object message) {
        String messageStr = (message instanceof String) ? (String) message : message.toString();
        log.debug("사용자 채널로 메시지 전송: {}", userId);
        redisTemplate.convertAndSend("user:" + userId, messageStr);
    }

    @Override
    public String subscribe(String destination, MessageCallback callback) {
        String subscriptionId = UUID.randomUUID().toString();
        
        ChannelTopic topic = new ChannelTopic(destination);
        MessageListener listener = (message, pattern) -> {
            String messageStr = new String(message.getBody());
            callback.onMessage(messageStr);
        };
        
        listenerContainer.addMessageListener(listener, topic);
        
        listeners.put(subscriptionId, listener);
        topics.put(subscriptionId, topic);
        
        log.debug("Redis 채널 구독 완료: {} (구독 ID: {})", destination, subscriptionId);
        return subscriptionId;
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        MessageListener listener = listeners.get(subscriptionId);
        ChannelTopic topic = topics.get(subscriptionId);
        
        if (listener != null && topic != null) {
            listenerContainer.removeMessageListener(listener, topic);
            listeners.remove(subscriptionId);
            topics.remove(subscriptionId);
            log.debug("Redis 구독 취소 완료 (구독 ID: {})", subscriptionId);
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void connect() {
        connected = true;
        log.debug("Redis 메시지 서비스 연결됨");
    }

    @Override
    public void disconnect() {
        connected = false;
        
        // 모든 구독 취소
        listeners.keySet().forEach(this::unsubscribe);
        listeners.clear();
        topics.clear();
        
        log.debug("Redis 메시지 서비스 연결 해제됨");
    }
} 