package com.ll.quizzle.domain.member.dto.response;

/**
 * 클라이언트에게 토큰 정보를 제공하는 DTO
 */
public record TokenInfoResponse(
    String accessToken,
    Long accessTokenExpiryTime,
    String refreshToken
) {

    public static TokenInfoResponse of(
            String accessToken,
            Long accessTokenExpiryTime,
            String refreshToken
    ) {
        return new TokenInfoResponse(accessToken, accessTokenExpiryTime, refreshToken);
    }
} 