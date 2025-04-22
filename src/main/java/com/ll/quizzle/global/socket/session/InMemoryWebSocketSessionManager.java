package com.ll.quizzle.global.socket.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.ll.quizzle.global.socket.core.SessionInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기존 메모리 기반 WebSocket 세션 관리 구현체 (Redis 방식 도입 시 사용 안할 예정, 현재는 개발 테스트 용도)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryWebSocketSessionManager implements WebSocketSessionManager {

    private final Map<String, Map<String, SessionInfo>> activeUserSessions = new ConcurrentHashMap<>();

    @Override
    public void registerSession(String email, String sessionId, String accessToken, Long expiryTime) {
        Map<String, SessionInfo> sessions = activeUserSessions.computeIfAbsent(email, k -> new ConcurrentHashMap<>());
        sessions.put(sessionId, new SessionInfo(accessToken, expiryTime, sessionId));
        log.debug("세션 등록: 사용자={}, 세션={}", email, sessionId);
    }

    @Override
    public void removeSession(String email, String sessionId) {
        Map<String, SessionInfo> sessions = activeUserSessions.get(email);
        if (sessions != null) {
            sessions.remove(sessionId);
            log.debug("세션 제거: 사용자={}, 세션={}", email, sessionId);
            
            if (sessions.isEmpty()) {
                activeUserSessions.remove(email);
                log.debug("사용자 세션 항목 제거: 사용자={}", email);
            }
        }
    }

    @Override
    public boolean isSessionValid(String email, String sessionId) {
        Map<String, SessionInfo> sessions = activeUserSessions.get(email);
        if (sessions == null) {
            return false;
        }
        
        SessionInfo sessionInfo = sessions.get(sessionId);
        if (sessionInfo == null) {
            return false;
        }
        
        return System.currentTimeMillis() < sessionInfo.expiryTime();
    }

    @Override
    public void removeExpiredSessions(long currentTime, BiConsumer<String, SessionInfo> expiredSessionCallback) {
        activeUserSessions.forEach((email, sessions) -> {
            sessions.entrySet().removeIf(entry -> {
                SessionInfo sessionInfo = entry.getValue();
                if (sessionInfo.expiryTime() < currentTime) {
                    log.debug("토큰 만료 감지 - 사용자: {}, 세션: {}", email, sessionInfo.sessionId());
                    
                    if (expiredSessionCallback != null) {
                        expiredSessionCallback.accept(email, sessionInfo);
                    }
                    
                    return true;
                }
                return false;
            });
            
            if (sessions.isEmpty()) {
                activeUserSessions.remove(email);
            }
        });
    }

    @Override
    public Map<String, Map<String, SessionInfo>> getActiveUserSessions() {
        Map<String, Map<String, SessionInfo>> result = new HashMap<>();

        for (Map.Entry<String, Map<String, SessionInfo>> entry : activeUserSessions.entrySet()) {
            result.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        
        return result;
    }

    @Override
    public String getSessionToTerminate(String email, String sessionId) {
        // InMemory 구현에서는 종료 대상 세션을 관리하지 않음 (Redis 구현에서만 사용)
        return null;
    }
    
    /**
     * InMemory 에선 세션을 바로 관리할 수 있기 때문에, 중복 로그인 시 하나의 세션만 유지 후 전부 종료
     * 룸 쪽을 돌면서 핸들링 하는 것 보다 세션을 관리하는 후크 메서드에서 처리하는게 더 효율적이라고 판단
     */
    @Override
    public int markOtherSessionsForTermination(String email, String sessionToKeep) {
        Map<String, SessionInfo> sessions = activeUserSessions.get(email);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        Set<String> sessionIdsToRemove = new HashSet<>();
        
        for (String sessionId : sessions.keySet()) {
            if (!sessionId.equals(sessionToKeep)) {
                sessionIdsToRemove.add(sessionId);
                count++;
            }
        }
        
        for (String sessionId : sessionIdsToRemove) {
            sessions.remove(sessionId);
            log.debug("다중 접속 감지 - 세션 즉시 제거: 사용자={}, 제거된 세션={}, 유지 세션={}", 
                    email, sessionId, sessionToKeep);
        }
        
        if (sessions.isEmpty()) {
            activeUserSessions.remove(email);
            log.debug("사용자 세션 항목 제거: 사용자={}", email);
        }
        
        return count;
    }

    @Override
    public boolean refreshSession(String email, String sessionId) {
        Map<String, SessionInfo> sessions = activeUserSessions.get(email);
        if (sessions == null || !sessions.containsKey(sessionId)) {
            log.debug("갱신 실패 - 세션 정보가 없음: 사용자={}, 세션={}", email, sessionId);
            return false;
        }
        
        SessionInfo sessionInfo = sessions.get(sessionId);
        if (sessionInfo == null) {
            log.debug("갱신 실패 - 세션 정보가 없음: 사용자={}, 세션={}", email, sessionId);
            return false;
        }
        
        long newExpiryTime = System.currentTimeMillis() + (3600 * 1000);
        SessionInfo updatedSessionInfo = new SessionInfo(
                sessionInfo.accessToken(),
                newExpiryTime,
                sessionInfo.sessionId()
        );
        
        sessions.put(sessionId, updatedSessionInfo);
        
        log.debug("세션 갱신 완료: 사용자={}, 세션={}", email, sessionId);
        return true;
    }
} 