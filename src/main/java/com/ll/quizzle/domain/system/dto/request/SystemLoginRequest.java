package com.ll.quizzle.domain.system.dto.request;

public record SystemLoginRequest(
	String systemEmail,
	String systemPassword
) {
	public static SystemLoginRequest of(
		String systemEmail,
		String systemPassword
	) {
		return new SystemLoginRequest(systemEmail, systemPassword);
	}
}
