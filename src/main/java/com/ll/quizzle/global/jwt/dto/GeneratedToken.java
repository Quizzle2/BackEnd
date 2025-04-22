package com.ll.quizzle.global.jwt.dto;

import lombok.Builder;

@Builder
public record GeneratedToken(
        String accessToken,
        String refreshToken
) {
}