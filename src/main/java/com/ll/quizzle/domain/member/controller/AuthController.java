package com.ll.quizzle.domain.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ll.quizzle.domain.member.dto.request.TokenRefreshRequest;
import com.ll.quizzle.domain.member.dto.response.TokenInfoResponse;
import com.ll.quizzle.domain.member.service.AuthService;
import com.ll.quizzle.global.response.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "토큰 관리", description = "토큰 정보 조회 및 갱신 관련 API (웹소켓 연결 유지를 위한 토큰 갱신 포함)")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/token-info")
    @Operation(summary = "토큰 정보 조회", description = "현재 사용자의 액세스 토큰과 리프레시 토큰 정보를 조회합니다.")
    public RsData<TokenInfoResponse> getTokenInfo(
            @CookieValue(value = "access_token", required = false) String accessToken, 
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        TokenInfoResponse tokenInfo = authService.getTokenInfo(accessToken, refreshToken);
        return RsData.success(HttpStatus.OK,  tokenInfo);
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "토큰 갱신", description = "만료된 액세스 토큰을 리프레시 토큰을 사용하여 갱신합니다. 웹소켓 연결 중 토큰이 만료된 경우에도 사용됩니다.")
    public RsData<TokenInfoResponse> refreshToken(
            @RequestBody TokenRefreshRequest request,
            @CookieValue(value = "refresh_token", required = false) String cookieRefreshToken,
            HttpServletResponse response) {
        
        String refreshToken = (request != null && request.refreshToken() != null) 
                ? request.refreshToken() 
                : cookieRefreshToken;
                
        TokenInfoResponse tokenInfo = authService.refreshToken(refreshToken, response);
        return RsData.success(HttpStatus.OK, tokenInfo);
    }
} 