package com.ll.quizzle.global.socket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class WebSocketNotificationServiceTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private WebSocketNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new WebSocketNotificationService(messagingTemplate);
    }

    @Test
    @DisplayName("토큰 만료 알림 전송 테스트")
    void testSendTokenExpiredNotification() {
        // given
        String email = "test@example.com";
        String expectedDestination = "/user/" + email + "/queue/token-expired";
        String expectedMessage = "토큰이 만료되었습니다. 다시 로그인해주세요.";

        // when
        notificationService.sendTokenExpiredNotification(email);

        // then
        verify(messagingTemplate, times(1))
                .convertAndSend(eq(expectedDestination), eq(expectedMessage));
    }

    @Test
    @DisplayName("알림 전송 실패 시나리오 테스트")
    void testNotificationFailure() {
        // given
        String email = "invalid@example.com";
        String expectedDestination = "/user/" + email + "/queue/token-expired";
        String expectedMessage = "토큰이 만료되었습니다. 다시 로그인해주세요.";
        doThrow(new RuntimeException("메시지 전송 실패"))
                .when(messagingTemplate)
                .convertAndSend(eq(expectedDestination), eq(expectedMessage));

        // when, then
        assertThrows(RuntimeException.class, () -> {
            notificationService.sendTokenExpiredNotification(email);
        });

        verify(messagingTemplate, times(1))
                .convertAndSend(eq(expectedDestination), eq(expectedMessage));
    }
}