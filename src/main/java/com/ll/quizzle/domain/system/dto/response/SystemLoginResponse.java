package com.ll.quizzle.domain.system.dto.response;

public record SystemLoginResponse(
	String accessToken,
	String refreshToken,
	String role
) {
}
