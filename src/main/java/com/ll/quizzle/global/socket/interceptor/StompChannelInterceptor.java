package com.ll.quizzle.global.socket.interceptor;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.global.security.oauth2.dto.SecurityUser;
import static com.ll.quizzle.global.security.oauth2.dto.SecurityUser.of;
import com.ll.quizzle.global.socket.security.WebSocketSecurityService;
import com.ll.quizzle.global.socket.service.WebSocketNotificationService;
import com.ll.quizzle.global.socket.session.WebSocketSessionManager;
import com.ll.quizzle.global.socket.session.WebSocketSessionRegistry;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final MemberService memberService;
    private final WebSocketSessionRegistry sessionRegistry;
    private final WebSocketNotificationService notificationService;
    private final WebSocketSecurityService securityService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("STOMP CONNECT 프레임 수신");
            
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.containsKey("email")) {
                String email = (String) sessionAttributes.get("email");
                Long memberId = (Long) sessionAttributes.get("memberId");
                String sessionId = (String) sessionAttributes.get("sessionId");
                String accessToken = (String) sessionAttributes.get("accessToken");
                Long tokenExpiryTime = (Long) sessionAttributes.get("tokenExpiryTime");
                String sessionSignature = (String) sessionAttributes.get("sessionSignature");
                
                log.debug("STOMP 연결 - 세션 속성에서 사용자 정보 복원: {}", email);
                
                String sessionData = email + ":" + memberId + ":" + tokenExpiryTime;
                boolean isSignatureValid = securityService.validateSignature(sessionData, sessionSignature);
                
                if (!isSignatureValid) {
                    log.debug("세션 서명 검증 실패 - 보안 위반 감지: {}", email);
                    return null;
                }
                
                Member member = memberService.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("Member not found"));
                
                SecurityUser userDto = of(
                        memberId,
                        member.getNickname(),
                        email,
                        "ROLE_MEMBER"
                );
                
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userDto,
                        null,
                        userDto.getAuthorities()
                );
                
                accessor.setUser(auth);
                
                String stompSessionId = accessor.getSessionId();
                log.debug("STOMP 세션 ID: {}, WebSocket 세션 ID: {}", stompSessionId, sessionId);
                
                sessionRegistry.getSessionManager().registerSession(email, stompSessionId, accessToken, tokenExpiryTime);
                
                log.debug("STOMP 연결 성공 - 사용자: {}", member.getEmail());
            } else {
                log.debug("STOMP 연결 - 인증 정보 없음");
            }
        }
        else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            if (accessor.getUser() != null) {
                String email = accessor.getUser().getName();
                sessionRegistry.getSessionManager().removeSession(email, accessor.getSessionId());
                log.debug("STOMP 연결 종료 - 사용자: {}", email);
            }
        }
        else if (StompCommand.SEND.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            String sessionId = accessor.getSessionId();
            
            if (principal != null) {
                String principalName = principal.getName();
                
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null && sessionAttributes.containsKey("email")) {
                    String email = (String) sessionAttributes.get("email");
                    
                    if (!sessionRegistry.getSessionManager().isSessionValid(email, sessionId)) {
                        log.debug("유효하지 않은 세션으로부터의 메시지: 닉네임={}, 이메일={}, 세션={}", 
                                  principalName, email, sessionId);
                        notificationService.sendTokenExpiredNotification(principalName);
                        return null;
                    }
                    
                    boolean refreshed = sessionRegistry.getSessionManager().refreshSession(email, sessionId);
                    
                    if (!refreshed) {
                        log.debug("세션 유효성 검사는 통과했으나 갱신 실패 - 데이터 불일치 감지: 사용자={}, 세션={}", 
                                email, sessionId);
                        notificationService.sendTokenExpiredNotification(principalName);
                        return null;
                    }
                    
                    log.debug("메시지 전송 허용: 닉네임={}, 이메일={}, 세션={}", 
                              principalName, email, sessionId);
                    return message;
                } else {
                    log.debug("세션 속성에 이메일 정보 없음: 닉네임={}, 세션={}", principalName, sessionId);
                    notificationService.sendTokenExpiredNotification(principalName);
                    return null;
                }
            } else {
                log.debug("인증되지 않은 메시지 요청 감지");
                return null;
            }
        }
        
        return message;
    }
}