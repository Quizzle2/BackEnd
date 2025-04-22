package com.ll.quizzle.global.socket.service;

import com.ll.quizzle.global.socket.core.MessageCallback;
import com.ll.quizzle.global.socket.service.stomp.StompMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class StompMessageServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private StompMessageService stompMessageService;

    @BeforeEach
    void setUp() {
        stompMessageService = new StompMessageService(messagingTemplate);
    }

    @Test
    @DisplayName("메시지 전송 테스트")
    void testSendMessage() {
        // given
        String destination = "/topic/test";
        String message = "테스트 메시지";

        // when
        stompMessageService.send(destination, message);

        // then
        verify(messagingTemplate, times(1)).convertAndSend(eq(destination), eq(message));
    }

    @Test
    @DisplayName("개인 메시지 전송 테스트")
    void testSendToUser() {
        // given
        String userId = "testUser";
        String message = "개인 메시지";

        // when
        stompMessageService.sendToUser(userId, message);

        // then
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(eq(userId), eq("/queue/private"), eq(message));
    }

    @Test
    @DisplayName("구독 및 콜백 테스트")
    void testSubscribeAndCallback() {
        // given
        String destination = "/topic/test";
        MessageCallback callback = mock(MessageCallback.class);

        // when
        String subscriptionId = stompMessageService.subscribe(destination, callback);
        stompMessageService.handleMessage(subscriptionId, "테스트 메시지");

        // then
        assertThat(subscriptionId).isNotNull();
        verify(callback, times(1)).onMessage(any());
    }

    @Test
    @DisplayName("연결 상태 테스트")
    void testConnectionState() {
        // given
        assertThat(stompMessageService.isConnected()).isFalse();

        // when
        stompMessageService.connect();

        // then
        assertThat(stompMessageService.isConnected()).isTrue();

        // when
        stompMessageService.disconnect();

        // then
        assertThat(stompMessageService.isConnected()).isFalse();
    }

    @Test
    @DisplayName("토픽 메시지 전송 테스트")
    void testSendToTopic() {
        // given
        String destination = "/topic/test";
        Map<String, Object> message = new HashMap<>();
        message.put("type", "test");
        message.put("content", "토픽 메시지");

        // when
        stompMessageService.sendToTopic(destination, message);

        // then
        verify(messagingTemplate, times(1))
                .convertAndSend(eq(destination), eq(message));
    }
} 