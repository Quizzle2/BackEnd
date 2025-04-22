package com.ll.quizzle.global.socket;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.ll.quizzle.global.socket.core.SessionInfo;
import com.ll.quizzle.global.socket.session.InMemoryWebSocketSessionManager;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class WebSocketSessionManagerTest {
    
    private InMemoryWebSocketSessionManager sessionManager;
    
    @Mock
    private BiConsumer<String, SessionInfo> mockCallback;
    
    private final String testEmail = "test@example.com";
    private final String testSessionId = "test-session-id";
    private final String testAccessToken = "test-access-token";
    private final long futureExpiryTime = System.currentTimeMillis() + 3600000; // 1시간 후
    private final long pastExpiryTime = System.currentTimeMillis() - 3600000; // 1시간 전
    
    @BeforeEach
    void setUp() {
        sessionManager = new InMemoryWebSocketSessionManager();
    }
    
    @Test
    @DisplayName("세션 등록 및 조회 테스트")
    void testRegisterAndGetSession() {
        // given
        // when
        sessionManager.registerSession(testEmail, testSessionId, testAccessToken, futureExpiryTime);
        
        // then
        Map<String, Map<String, SessionInfo>> activeSessions = sessionManager.getActiveUserSessions();
        assertThat(activeSessions).isNotEmpty();
        assertThat(activeSessions).containsKey(testEmail);
        
        Map<String, SessionInfo> userSessions = activeSessions.get(testEmail);
        assertThat(userSessions).containsKey(testSessionId);
        
        SessionInfo sessionInfo = userSessions.get(testSessionId);
        assertThat(sessionInfo.accessToken()).isEqualTo(testAccessToken);
        assertThat(sessionInfo.expiryTime()).isEqualTo(futureExpiryTime);
        assertThat(sessionInfo.sessionId()).isEqualTo(testSessionId);
    }
    
    @Test
    @DisplayName("세션 삭제 테스트")
    void testRemoveSession() {
        // given
        sessionManager.registerSession(testEmail, testSessionId, testAccessToken, futureExpiryTime);
        
        // when
        sessionManager.removeSession(testEmail, testSessionId);
        
        // then
        Map<String, Map<String, SessionInfo>> activeSessions = sessionManager.getActiveUserSessions();
        assertThat(activeSessions).isEmpty();
    }
    
    @Test
    @DisplayName("유효한 세션 검증 테스트")
    void testIsSessionValid() {
        // given
        sessionManager.registerSession(testEmail, testSessionId, testAccessToken, futureExpiryTime);
        
        // when
        boolean isValid = sessionManager.isSessionValid(testEmail, testSessionId);
        
        // then
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("만료된 세션 검증 테스트")
    void testExpiredSession() {
        // given
        sessionManager.registerSession(testEmail, testSessionId, testAccessToken, pastExpiryTime);
        
        // when
        boolean isValid = sessionManager.isSessionValid(testEmail, testSessionId);
        
        // then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("만료된 세션 자동 삭제 테스트")
    void testRemoveExpiredSessions() {
        // given
        sessionManager.registerSession(testEmail, testSessionId, testAccessToken, pastExpiryTime);
        String validSessionId = "valid-session-id";
        sessionManager.registerSession(testEmail, validSessionId, testAccessToken, futureExpiryTime);
        
        // when
        sessionManager.removeExpiredSessions(System.currentTimeMillis(), mockCallback);
        
        // then
        Map<String, Map<String, SessionInfo>> activeSessions = sessionManager.getActiveUserSessions();
        assertThat(activeSessions).isNotEmpty();
        assertThat(activeSessions).containsKey(testEmail);
        
        Map<String, SessionInfo> userSessions = activeSessions.get(testEmail);
        assertThat(userSessions).doesNotContainKey(testSessionId);
        assertThat(userSessions).containsKey(validSessionId);
        
        verify(mockCallback, times(1)).accept(eq(testEmail), any(SessionInfo.class));
    }
    
    @Test
    @DisplayName("콜백 함수 실행 테스트")
    void testExpiredSessionCallback() {
        // given
        sessionManager.registerSession(testEmail, testSessionId, testAccessToken, pastExpiryTime);
        
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        BiConsumer<String, SessionInfo> localCallback = (email, sessionInfo) -> {
            assertThat(email).isEqualTo(testEmail);
            assertThat(sessionInfo.sessionId()).isEqualTo(testSessionId);
            callbackExecuted.set(true);
        };
        
        // when
        sessionManager.removeExpiredSessions(System.currentTimeMillis(), localCallback);
        
        // then
        assertThat(callbackExecuted.get()).isTrue();
    }
    
    @Test
    @DisplayName("다수의 세션 관리 테스트")
    void testMultipleSessionsManagement() {
        // given
        String anotherEmail = "another@example.com";
        String anotherSessionId = "another-session-id";
        
        sessionManager.registerSession(testEmail, testSessionId, testAccessToken, futureExpiryTime);
        sessionManager.registerSession(anotherEmail, anotherSessionId, testAccessToken, futureExpiryTime);
        
        // when
        Map<String, Map<String, SessionInfo>> activeSessions = sessionManager.getActiveUserSessions();
        
        // then
        assertThat(activeSessions).hasSize(2);
        assertThat(activeSessions).containsKeys(testEmail, anotherEmail);
    }
    
    @Test
    @DisplayName("세션 갱신 테스트")
    void testRefreshSession() {
        // given
        sessionManager.registerSession(testEmail, testSessionId, testAccessToken, pastExpiryTime);
        
        // when
        boolean refreshed = sessionManager.refreshSession(testEmail, testSessionId);
        
        // then
        assertThat(refreshed).isTrue();
        
        boolean isValid = sessionManager.isSessionValid(testEmail, testSessionId);
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("다중 세션 종료 표시 테스트")
    void testMarkOtherSessionsForTermination() {
        // given
        String sessionId1 = "session-1";
        String sessionId2 = "session-2";
        String sessionId3 = "session-3";
        
        sessionManager.registerSession(testEmail, sessionId1, testAccessToken, futureExpiryTime);
        sessionManager.registerSession(testEmail, sessionId2, testAccessToken, futureExpiryTime);
        sessionManager.registerSession(testEmail, sessionId3, testAccessToken, futureExpiryTime);
        
        // when
        int markedCount = sessionManager.markOtherSessionsForTermination(testEmail, sessionId1);
        
        // then
        assertThat(markedCount).isEqualTo(2);
        
        Map<String, Map<String, SessionInfo>> activeSessions = sessionManager.getActiveUserSessions();
        Map<String, SessionInfo> userSessions = activeSessions.get(testEmail);
        
        assertThat(userSessions).containsKey(sessionId1);
        assertThat(userSessions).doesNotContainKey(sessionId2);
        assertThat(userSessions).doesNotContainKey(sessionId3);
    }
} 