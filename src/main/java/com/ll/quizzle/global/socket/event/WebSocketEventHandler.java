package com.ll.quizzle.global.socket.event;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.domain.room.service.RoomService;
import com.ll.quizzle.global.socket.core.MessageService;
import com.ll.quizzle.global.socket.core.MessageServiceFactory;
import com.ll.quizzle.global.socket.core.SessionInfo;
import com.ll.quizzle.global.socket.service.WebSocketNotificationService;
import com.ll.quizzle.global.socket.session.WebSocketSessionManager;
import com.ll.quizzle.global.socket.session.WebSocketSessionRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 전체적인 WebSocket 세션 연결 및 종료 이벤트를 처리하는 핸들러
 * 세션 등록 및 해제, 연결 끊김 처리를 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventHandler {

    private final WebSocketSessionRegistry sessionRegistry;
    private final MemberService memberService;
    private final WebSocketNotificationService notificationService;
    private final RoomService roomService;
    private final MessageServiceFactory messageServiceFactory;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onSessionConnectEvent(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        
        if (sessionAttributes != null && sessionAttributes.containsKey("email")) {
            String email = (String) sessionAttributes.get("email");
            String accessToken = (String) sessionAttributes.get("accessToken");
            String stompSessionId = accessor.getSessionId();
            Long expiryTime = (Long) sessionAttributes.get("tokenExpiryTime");
            String sessionId = (String) sessionAttributes.get("sessionId");
            
            log.debug("세션 연결 이벤트: 사용자={}, STOMP 세션={}, HTTP 세션/토큰 ID={}", 
                email, stompSessionId, sessionId);
            
            sessionRegistry.getSessionManager().registerSession(email, stompSessionId, accessToken, expiryTime);

            if (sessionId != null && sessionId.startsWith("token-")) {
                log.debug("토큰 기반 세션 감지 - 다른 세션 종료 처리 스킵: 사용자={}, 세션={}", 
                    email, stompSessionId);
            } else {
                int markedSessions = sessionRegistry.getSessionManager().markOtherSessionsForTermination(email, stompSessionId);
                if (markedSessions > 0) {
                    log.debug("다중 접속 감지 - 이전 세션 종료 처리: 사용자={}, 새 세션={}, 종료할 세션 수={}",
                            email, stompSessionId, markedSessions);
                }
            }
            
            if (!memberService.verifyToken(accessToken)) {
                log.debug("세션 연결 시 토큰 만료 감지: 사용자={}", email);
                notificationService.sendTokenExpiredNotification(email);
            }

            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(300);
                    broadcastActiveUsers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    @EventListener
    public void onSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("email")) {
            String email = (String) sessionAttributes.get("email");
            Long memberId = (Long) sessionAttributes.get("memberId");
            log.debug("세션 종료 이벤트: 이메일={}, 멤버ID={}, 세션={}", email, memberId, sessionId);

            String terminatingSessionId = sessionRegistry.getSessionManager().getSessionToTerminate(email, sessionId);
            if (terminatingSessionId != null) {
                log.debug("다른 기기 접속으로 인한 세션 종료: 사용자={}, 종료 세션={}, 새 세션={}",
                        email, sessionId, terminatingSessionId);

                notificationService.sendDuplicateLoginNotification(email, sessionId);
            }

            try {
                roomService.handleDisconnect(memberId);
                log.debug("방 연결 해제 처리 완료: 멤버ID={}", memberId);
            } catch (Exception e) {
                log.error("방 연결 해제 처리 중 오류 발생: {}", e.getMessage(), e);
            }

            try {
                sessionRegistry.getSessionManager().removeSession(email, sessionId);
                log.debug("세션 제거 완료: 이메일={}, 세션={}", email, sessionId);
            } catch (Exception e) {
                log.error("세션 제거 중 오류 발생: {}", e.getMessage(), e);
                return;
            }

            try {
                broadcastActiveUsers();
                log.debug("접속자 목록 브로드캐스트 완료");
            } catch (Exception e) {
                log.error("접속자 목록 브로드캐스트 중 오류 발생: {}", e.getMessage(), e);
            }

            return;
        }
        
        Principal principal = accessor.getUser();
        if (principal != null) {
            String principalName = principal.getName();
            log.debug("세션 종료 이벤트: 닉네임={}, 세션={}", principalName, sessionId);
            log.debug("이메일 정보 없음 - 세션 정리 실패");
        }
    }


    private void broadcastActiveUsers() {
        try {
            WebSocketSessionManager sessionManager = sessionRegistry.getSessionManager();
            Map<String, Map<String, SessionInfo>> activeSessions = sessionManager.getActiveUserSessions();
            List<Map<String, Object>> activeUsers = convertToUsersList(activeSessions);

            String usersJson = objectMapper.writeValueAsString(activeUsers);

            MessageService roomService = messageServiceFactory.getRoomService();
            roomService.send("/topic/lobby/users", usersJson);

            log.debug("접속자 정보 브로드캐스트: {} 명", activeUsers.size());
        } catch (Exception e) {
            log.error("접속자 정보 브로드캐스트 실패: {}", e.getMessage());
        }
    }


    private List<Map<String, Object>> convertToUsersList(Map<String, Map<String, SessionInfo>> activeSessions) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, Map<String, SessionInfo>> entry : activeSessions.entrySet()) {
            String email = entry.getKey();

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("email", email);

            memberService.findByEmail(email).ifPresent(member -> {
                userInfo.put("nickname", member.getNickname());
                userInfo.put("id", member.getId());
            });

            List<String> sessionIds = new ArrayList<>(entry.getValue().keySet());
            userInfo.put("sessions", sessionIds);

            userInfo.put("lastActive", System.currentTimeMillis());

            userInfo.put("status", "online");

            result.add(userInfo);
        }

        return result;
    }
}