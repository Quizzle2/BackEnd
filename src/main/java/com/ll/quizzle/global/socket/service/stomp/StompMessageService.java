package com.ll.quizzle.global.socket.service.stomp;

import com.ll.quizzle.global.socket.core.MessageCallback;
import com.ll.quizzle.global.socket.core.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP 기반 WebSocket 메시징 서비스 구현체
 * 현재는 로비 및 룸에서 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StompMessageService implements MessageService {

    private final SimpMessagingTemplate messagingTemplate;
    /**
     * 공유자원 이라, 동시성 문제를 고려하여 ConcurrentHashMap 을 사용했습니다.
     */
    private final Map<String, MessageCallback> subscriptions = new ConcurrentHashMap<>();
    private boolean connected = false;

    @Override
    public void send(String destination, Object message) {
        log.debug("메시지 전송 to : {}", destination);
        messagingTemplate.convertAndSend(destination, message);
    }

    @Override
    public void sendToUser(String userId, Object message) {
        log.debug("개인 메시지 전송 to : {}", userId);
        messagingTemplate.convertAndSendToUser(userId, "/queue/private", message);
    }

    @Override
    public String subscribe(String destination, MessageCallback callback) {
        String subscriptionId = UUID.randomUUID().toString();
        subscriptions.put(subscriptionId, callback);
        log.debug("구독 등록 : {} with ID: {}", destination, subscriptionId);
        
        return subscriptionId;
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        subscriptions.remove(subscriptionId);
        log.debug("구독 취소 ID : {}", subscriptionId);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void connect() {
        connected = true;
        log.debug("STOMP 서비스에 연결되었습니다.");
    }

    @Override
    public void disconnect() {
        connected = false;
        subscriptions.clear();
        log.debug("STOMP 서비스 연결이 해제되었습니다.");
    }


    public void handleMessage(String subscriptionId, Object message) {
        MessageCallback callback = subscriptions.get(subscriptionId);
        if (callback != null) {
            callback.onMessage(message);
        }
    }

    public void sendToTopic(String destination, Map<String, Object> message) {
        log.debug("토픽 메시지 전송 to : {}", destination);
        messagingTemplate.convertAndSend(destination, message);
    }
} 