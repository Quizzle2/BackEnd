package com.ll.quizzle.global.socket.scheduler;

import com.ll.quizzle.global.socket.core.SessionInfo;
import com.ll.quizzle.global.socket.service.WebSocketNotificationService;
import com.ll.quizzle.global.socket.session.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.function.BiConsumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TokenExpirySchedulerTest {

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private WebSocketNotificationService notificationService;

    private TokenExpiryScheduler tokenExpiryScheduler;

    @BeforeEach
    void setUp() {
        tokenExpiryScheduler = new TokenExpiryScheduler(sessionManager, notificationService);
        ReflectionTestUtils.setField(tokenExpiryScheduler, "tokenExpiryCheckingSeconds", 1);
    }

    @Test
    @DisplayName("스케줄러 초기화 테스트")
    void testInitializeScheduler() {
        // when
        tokenExpiryScheduler.init();

        // then
        verify(sessionManager, timeout(2000).atLeastOnce()).removeExpiredSessions(anyLong(), any());
    }

    @Test
    @DisplayName("만료된 세션 처리 콜백 테스트")
    void testExpiredSessionCallback() throws Exception {
        // given
        final String testEmail = "test@example.com";
        final String testSessionId = "test-session-id";
        final String testToken = "test-token";
        final long expiryTime = Instant.now().minusSeconds(60).toEpochMilli();

        doAnswer(invocation -> {
            BiConsumer<String, SessionInfo> callback = invocation.getArgument(1);
            SessionInfo sessionInfo = new SessionInfo(testToken, expiryTime, testSessionId);
            callback.accept(testEmail, sessionInfo);
            return null;
        }).when(sessionManager).removeExpiredSessions(anyLong(), any());

        // when
        tokenExpiryScheduler.init();
        
        // then
        verify(notificationService, timeout(2000)).sendTokenExpiredNotification(eq(testEmail));
    }

    @Test
    @DisplayName("토큰 만료 검사 주기 설정 테스트")
    void testExpiryCheckingPeriod() {
        // given
        long period = 5; 
        ReflectionTestUtils.setField(tokenExpiryScheduler, "tokenExpiryCheckingSeconds", period);

        // when
        tokenExpiryScheduler.init();

        // then
        // 주기 설정이 올바르게 적용되었는지 검증하기 어려움
        // 간접적으로 스케줄러가 초기화되고 호출되는지 검증
        verify(sessionManager, timeout(period * 1000 + 1000).atLeastOnce()).removeExpiredSessions(anyLong(), any());
    }
} 