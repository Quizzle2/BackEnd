package com.ll.quizzle.global.socket.service.redis;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageListener implements MessageListener {

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    
    private final Map<String, ChannelTopic> activeChannels = new ConcurrentHashMap<>();

    private final List<String> FIXED_TOPICS = List.of(
            "/topic/lobby/chat"
    );

    @PostConstruct
    public void init() {
        for (String topic : FIXED_TOPICS) {
            subscribeToChannel(topic);
        }
        
        log.debug("Redis 메시지 리스너 초기화 완료, 고정 채널 {} 개 구독됨", FIXED_TOPICS.size());
    }


    public void subscribeToChannel(String channel) {
        if (!activeChannels.containsKey(channel)) {
            ChannelTopic topic = new ChannelTopic(channel);
            redisMessageListenerContainer.addMessageListener(this, topic);
            activeChannels.put(channel, topic);
            log.debug("Redis 채널 구독 완료: {}", channel);
        } else {
            log.debug("이미 구독 중인 채널: {}", channel);
        }
    }
    

    public void unsubscribeFromChannel(String channel) {
        ChannelTopic topic = activeChannels.remove(channel);
        if (topic != null) {
            redisMessageListenerContainer.removeMessageListener(this, topic);
            log.debug("Redis 채널 구독 해제 완료: {}", channel);
        } else {
            log.debug("구독되지 않은 채널 해제 시도: {}", channel);
        }
    }
    

    public void subscribeToRoomChannel(String roomId) {
        String chatChannel = "/topic/room/chat/" + roomId;
        subscribeToChannel(chatChannel);
        
        log.debug("방 {} 채팅 채널 구독 완료", roomId);
    }
    

    public void unsubscribeFromRoomChannel(String roomId) {
        String chatChannel = "/topic/room/chat/" + roomId;
        unsubscribeFromChannel(chatChannel);
        
        log.debug("방 {} 채팅 채널 구독 해제 완료", roomId);
    }


    public List<String> getActiveChannels() {
        return activeChannels.keySet().stream().toList();
    }

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            
            log.debug("Redis 메시지 수신: 채널={}, 원본 메시지={}", channel, messageBody);
            
            messagingTemplate.convertAndSend(channel, messageBody);
            log.debug("STOMP 클라이언트로 메시지 전달 완료: {}", channel);
        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 오류: {}", e.getMessage(), e);
        }
    }
} 