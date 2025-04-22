package com.ll.quizzle.global.socket.session;

import com.ll.quizzle.global.socket.core.SessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * 분산 환경에서 세션 정보를 공유할 수 있도록 Redis 를 사용하여 세션 관리
 */
@Primary
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWebSocketSessionManager implements WebSocketSessionManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_KEY_PREFIX = "ws:session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "ws:user:";
    private static final long SESSION_EXPIRY = 3600;
    private static final long SESSION_TERMINATE_EXPIRY = 10;

    @Override
    public void registerSession(String email, String sessionId, String accessToken, Long expiryTime) {

        SessionInfo sessionInfo = new SessionInfo(accessToken, expiryTime, sessionId);

        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + email;
        Map<Object, Object> existingSessions = redisTemplate.opsForHash().entries(userSessionsKey);

        boolean isTokenBasedSession = sessionId.startsWith("token-");

        if (!existingSessions.isEmpty()) {
            log.debug("기존 세션 감지: 사용자={}, 세션 수={}", email, existingSessions.size());
            
            boolean hasSameTokenSession = false;
            
            for (Object oldSessionIdObj : existingSessions.keySet()) {
                String oldSessionId = (String) oldSessionIdObj;
                
                if (sessionId.equals(oldSessionId)) {
                    continue;
                }
                
                String oldSessionKey = SESSION_KEY_PREFIX + oldSessionId;
                SessionInfo oldSessionInfo = (SessionInfo) redisTemplate.opsForValue().get(oldSessionKey);
                
                if (oldSessionInfo != null && accessToken.equals(oldSessionInfo.accessToken())) {
                    hasSameTokenSession = true;
                    log.debug("동일 토큰 세션 감지: 사용자={}, 기존 세션={}, 새 세션={}", 
                            email, oldSessionId, sessionId);
                    
                    if (isTokenBasedSession || oldSessionId.startsWith("token-") || 
                            System.currentTimeMillis() - (Long)existingSessions.get(oldSessionId) < 10000) {
                        log.debug("최근 연결된 세션으로 판단하여 종료 처리 안 함: {}", oldSessionId);
                        continue;
                    }
                }
                
                log.debug("다중 접속 감지 - 기존 세션 정보 저장: 사용자={}, 종료 대상 세션={}, 새 세션={}",
                        email, oldSessionId, sessionId);

                String sessionToTerminateKey = SESSION_KEY_PREFIX + oldSessionId + ":terminate";
                redisTemplate.opsForValue().set(sessionToTerminateKey, sessionId, SESSION_TERMINATE_EXPIRY, TimeUnit.SECONDS);
            }
        }

        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.opsForValue().set(sessionKey, sessionInfo, SESSION_EXPIRY, TimeUnit.SECONDS);

        redisTemplate.opsForHash().put(userSessionsKey, sessionId, System.currentTimeMillis());
        redisTemplate.expire(userSessionsKey, SESSION_EXPIRY, TimeUnit.SECONDS);

        log.debug("Redis에 세션 등록: 사용자={}, 세션={}", email, sessionId);
    }


    @Override
    public void removeSession(String email, String sessionId) {

        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.delete(sessionKey);

        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + email;
        redisTemplate.opsForHash().delete(userSessionsKey, sessionId);

        if (Boolean.TRUE.equals(redisTemplate.opsForHash().size(userSessionsKey) == 0)) {
            redisTemplate.delete(userSessionsKey);
            log.debug("사용자 세션 목록 삭제: 사용자={}", email);
        }

        log.debug("Redis에서 세션 제거: 사용자={}, 세션={}", email, sessionId);
    }


    @Override
    public boolean isSessionValid(String email, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        SessionInfo sessionInfo = (SessionInfo) redisTemplate.opsForValue().get(sessionKey);

        if (sessionInfo == null) {
            return false;
        }

        return System.currentTimeMillis() < sessionInfo.expiryTime();
    }


    @Override
    public void removeExpiredSessions(long currentTime, BiConsumer<String, SessionInfo> expiredSessionCallback) {

        Set<String> userKeys = redisTemplate.keys(USER_SESSIONS_KEY_PREFIX + "*");

        if (userKeys == null || userKeys.isEmpty()) {
            return;
        }

        for (String userKey : userKeys) {
            String email = userKey.substring(USER_SESSIONS_KEY_PREFIX.length());
            Map<Object, Object> sessions = redisTemplate.opsForHash().entries(userKey);

            for (Map.Entry<Object, Object> entry : sessions.entrySet()) {
                String sessionId = (String) entry.getKey();
                String sessionKey = SESSION_KEY_PREFIX + sessionId;
                SessionInfo sessionInfo = (SessionInfo) redisTemplate.opsForValue().get(sessionKey);

                if (sessionInfo != null && sessionInfo.expiryTime() < currentTime) {
                    log.debug("만료된 세션 발견: 사용자={}, 세션={}", email, sessionId);

                    if (expiredSessionCallback != null) {
                        expiredSessionCallback.accept(email, sessionInfo);
                    }

                    removeSession(email, sessionId);
                }
            }
        }
    }



    @Override
    public Map<String, Map<String, SessionInfo>> getActiveUserSessions() {
        Set<String> userKeys = redisTemplate.keys(USER_SESSIONS_KEY_PREFIX + "*");
        Map<String, Map<String, SessionInfo>> result = new HashMap<>();

        if (userKeys == null || userKeys.isEmpty()) {
            return result;
        }

        for (String userKey : userKeys) {
            String email = userKey.substring(USER_SESSIONS_KEY_PREFIX.length());
            Map<String, SessionInfo> userSessions = getUserSessions(email);

            if (!userSessions.isEmpty()) {
                result.put(email, userSessions);
            }
        }

        return result;
    }


    @Override
    public String getSessionToTerminate(String email, String sessionId) {
        String terminateKey = SESSION_KEY_PREFIX + sessionId + ":terminate";
        String newSessionId = (String) redisTemplate.opsForValue().get(terminateKey);

        if (newSessionId != null) {
            redisTemplate.delete(terminateKey);
            log.debug("세션 종료 요청 감지 및 처리 완료: 사용자={}, 종료 세션={}, 새 세션={}",
                    email, sessionId, newSessionId);
        }

        return newSessionId;
    }


    @Override
    public int markOtherSessionsForTermination(String email, String sessionToKeep) {
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + email;
        Map<Object, Object> allSessions = redisTemplate.opsForHash().entries(userSessionsKey);
        
        if (allSessions.isEmpty()) {
            return 0;
        }
        
        int markedCount = 0;
        
        for (Object sessionIdObj : allSessions.keySet()) {
            String sessionId = (String) sessionIdObj;
            
            if (sessionId.equals(sessionToKeep)) {
                continue;
            }
            
            if (sessionId.startsWith("token-")) {
                log.debug("토큰 기반 세션 감지 - 종료 처리 제외: 세션={}", sessionId);
                continue;
            }
            
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            SessionInfo sessionInfo = (SessionInfo) redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionInfo == null) {
                continue;
            }
            
            long lastActiveTime = (Long) allSessions.get(sessionId);
            if (System.currentTimeMillis() - lastActiveTime < 10000) {
                log.debug("최근 생성된 세션 감지 - 종료 처리 제외: 세션={}, 경과시간={}ms", 
                    sessionId, System.currentTimeMillis() - lastActiveTime);
                continue;
            }
            
            String sessionToTerminateKey = SESSION_KEY_PREFIX + sessionId + ":terminate";
            redisTemplate.opsForValue().set(sessionToTerminateKey, sessionToKeep, SESSION_TERMINATE_EXPIRY, TimeUnit.SECONDS);
            
            log.debug("세션 종료 대상으로 표시: 사용자={}, 종료 대상 세션={}, 유지할 세션={}", 
                email, sessionId, sessionToKeep);
            
            markedCount++;
        }
        
        return markedCount;
    }


    @Override
    public boolean refreshSession(String email, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        SessionInfo sessionInfo = (SessionInfo) redisTemplate.opsForValue().get(sessionKey);

        if (sessionInfo == null) {
            log.debug("갱신 실패 - 세션 정보가 없음: 사용자={}, 세션={}", email, sessionId);
            return false;
        }

        redisTemplate.expire(sessionKey, SESSION_EXPIRY, TimeUnit.SECONDS);

        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + email;

        if (Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(userSessionsKey, sessionId))) {
            redisTemplate.opsForHash().put(userSessionsKey, sessionId, System.currentTimeMillis());
            redisTemplate.expire(userSessionsKey, SESSION_EXPIRY, TimeUnit.SECONDS);

            log.debug("세션 갱신 완료: 사용자={}, 세션={}", email, sessionId);
            return true;
        }

        log.debug("갱신 실패 - 사용자 세션 목록에 세션이 없음: 사용자={}, 세션={}", email, sessionId);
        return false;
    }

    private Map<String, SessionInfo> getUserSessions(String email) {
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + email;
        Map<Object, Object> sessionIds = redisTemplate.opsForHash().entries(userSessionsKey);

        Map<String, SessionInfo> result = new HashMap<>();

        for (Object sessionIdObj : sessionIds.keySet()) {
            String sessionId = (String) sessionIdObj;
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            SessionInfo sessionInfo = (SessionInfo) redisTemplate.opsForValue().get(sessionKey);

            if (sessionInfo != null) {
                result.put(sessionId, sessionInfo);
            }
        }

        return result;
    }
}