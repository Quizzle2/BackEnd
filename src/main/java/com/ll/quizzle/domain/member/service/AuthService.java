package com.ll.quizzle.domain.member.service;

import org.springframework.stereotype.Service;

import com.ll.quizzle.domain.member.dto.response.TokenInfoResponse;
import static com.ll.quizzle.global.exceptions.ErrorCode.INTERNAL_SERVER_ERROR;
import static com.ll.quizzle.global.exceptions.ErrorCode.REFRESH_TOKEN_NOT_FOUND;
import static com.ll.quizzle.global.exceptions.ErrorCode.TOKEN_INFO_NOT_FOUND;
import com.ll.quizzle.global.jwt.dto.JwtProperties;
import com.ll.quizzle.standard.util.CookieUtil;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final MemberService memberService;
    private final JwtProperties jwtProperties;

    public TokenInfoResponse getTokenInfo(String accessToken, String refreshToken) {
        if (accessToken == null || refreshToken == null) {
            throw TOKEN_INFO_NOT_FOUND.throwServiceException();
        }
        
        try {
            Long expiryTime = memberService.getTokenExpiryTime(accessToken);
            return TokenInfoResponse.of(accessToken, expiryTime, refreshToken);
        } catch (Exception e) {
            log.error("토큰 정보 조회 중 오류 발생", e);
            throw INTERNAL_SERVER_ERROR.throwServiceException(e);
        }
    }

    public TokenInfoResponse refreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            throw REFRESH_TOKEN_NOT_FOUND.throwServiceException();
        }
        
        try {
            log.debug("토큰 갱신 시도 - 리프레시 토큰: {}", refreshToken);
            
            String newAccessToken = memberService.refreshAccessToken(refreshToken)
                    .getData();
            Long expiryTime = memberService.getTokenExpiryTime(newAccessToken);
            
            CookieUtil.addCookie(
                response, 
                "access_token", 
                newAccessToken, 
                (int) jwtProperties.getAccessTokenExpiration(), 
                true, 
                true
            );
            
            return TokenInfoResponse.of(newAccessToken, expiryTime, refreshToken);
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생", e);
            throw INTERNAL_SERVER_ERROR.throwServiceException(e);
        }
    }
} 