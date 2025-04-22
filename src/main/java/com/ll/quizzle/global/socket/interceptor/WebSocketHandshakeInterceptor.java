package com.ll.quizzle.global.socket.interceptor;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.global.exceptions.ErrorCode;
import static com.ll.quizzle.global.exceptions.ErrorCode.MEMBER_NOT_FOUND;
import static com.ll.quizzle.global.exceptions.ErrorCode.WEBSOCKET_ACCESS_TOKEN_NOT_FOUND;
import static com.ll.quizzle.global.exceptions.ErrorCode.WEBSOCKET_INVALID_REQUEST_TYPE;
import static com.ll.quizzle.global.exceptions.ErrorCode.WEBSOCKET_TOKEN_VALIDATION_FAILED;
import com.ll.quizzle.global.socket.security.WebSocketSecurityService;
import com.ll.quizzle.standard.util.CookieUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final MemberService memberService;
    private final WebSocketSecurityService securityService;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, 
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.debug("WebSocket 요청 타입 불일치: {}", request.getClass().getSimpleName());
            setErrorResponse(response, WEBSOCKET_INVALID_REQUEST_TYPE);
            return false;
        }

        HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
        
        String accessToken = httpServletRequest.getParameter("access_token");
        
        if (accessToken == null || accessToken.isEmpty()) {
            if (httpServletRequest.getCookies() == null) {
                log.debug("WebSocket 연결 시도 - 쿠키 및 URL 파라미터에 토큰 없음");
                setErrorResponse(response, WEBSOCKET_ACCESS_TOKEN_NOT_FOUND);
                return false;
            }
            
            Optional<Cookie> accessTokenCookie = CookieUtil.getCookie(httpServletRequest, "access_token");
            if (accessTokenCookie.isEmpty()) {
                log.debug("WebSocket 연결 시도 - 액세스 토큰 없음");
                setErrorResponse(response, WEBSOCKET_ACCESS_TOKEN_NOT_FOUND);
                return false;
            }
            
            accessToken = accessTokenCookie.get().getValue();
        }
        
        log.debug("WebSocket 연결 시도 - 액세스 토큰 발견");

        try {
            String email = memberService.extractEmailIfValid(accessToken);
            Member member = memberService.findByEmail(email)
                    .orElseThrow(MEMBER_NOT_FOUND::throwServiceException);
            
            Long tokenExpiryTime = memberService.getTokenExpiryTime(accessToken);
            
            attributes.put("email", email);
            attributes.put("memberId", member.getId());
            attributes.put("accessToken", accessToken);
            attributes.put("tokenExpiryTime", tokenExpiryTime);
            
            String sessionId = null;
            jakarta.servlet.http.HttpSession session = httpServletRequest.getSession(false);
            if (session != null) {
                sessionId = session.getId();
                log.debug("WebSocket 연결 시도 - 기존 세션 사용: {}", sessionId);
            } else {
                sessionId = "token-" + email + "-" + System.currentTimeMillis();
                log.debug("WebSocket 연결 시도 - 세션 없음, 토큰 기반 식별자 생성: {}", sessionId);
            }
            attributes.put("sessionId", sessionId);
            
            String sessionData = email + ":" + member.getId() + ":" + tokenExpiryTime;
            String signature = securityService.generateSignature(sessionData);
            attributes.put("sessionSignature", signature);
            
            log.debug("WebSocket 연결 성공 - 사용자: {}", email);
            return true;
        } catch (Exception e) {
            log.error("WebSocket 토큰 검증 실패: {}", e.getMessage());
            setErrorResponse(response, WEBSOCKET_TOKEN_VALIDATION_FAILED);
            return false;
        }
    }

    private void setErrorResponse(ServerHttpResponse response, ErrorCode errorCode) {
        response.setStatusCode(errorCode.getHttpStatus());
        response.getHeaders().add("X-WebSocket-Error", errorCode.getMessage());
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                              @NonNull WebSocketHandler wsHandler, Exception exception) {
        // 후크 메서드 입니다. 추후에 로그 같은것들 추가할 예정 입니다.
    }
} 