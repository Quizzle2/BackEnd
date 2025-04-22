package com.ll.quizzle.domain.member.dto.request;

import com.ll.quizzle.global.exceptions.ErrorCode;

/**
 * 클라이언트로부터 리프레시 토큰을 받기 위한 DTO
 */
public record TokenRefreshRequest(
    String refreshToken
) {
    public TokenRefreshRequest {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            ErrorCode.REFRESH_TOKEN_NOT_FOUND.throwServiceException();
        }
    }

    public static TokenRefreshRequest of(String refreshToken) {
        return new TokenRefreshRequest(refreshToken);
    }
}