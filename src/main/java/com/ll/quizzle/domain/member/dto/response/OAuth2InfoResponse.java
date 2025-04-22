package com.ll.quizzle.domain.member.dto.response;

public record OAuth2InfoResponse(
        String email,
        String name,
        String status,
        String role,
        String provider,
        String oauthId,
        boolean isMember,
        boolean isAdmin,
        String accessToken,
        String refreshToken
) {

}
