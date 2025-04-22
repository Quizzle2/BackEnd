package com.ll.quizzle.domain.system.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ll.quizzle.global.config.SystemProperties;
import com.ll.quizzle.global.exceptions.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SystemVerifier {
	private final PasswordEncoder passwordEncoder;
	private final SystemProperties systemProperties;

	// 시스템 이메일 검증
	public void verifySystemEmail(String inputEmail) {
		if (!systemProperties.getSystemEmail().equals(inputEmail)) {
			ErrorCode.INVALID_LOGIN_CREDENTIALS.throwServiceException();
		}
	}

	// 시스템 비밀번호 검증
	public void verifySystemPassword(String inputPassword) {
		if (!passwordEncoder.matches(inputPassword, systemProperties.getSystemPasswordHash())) {
			ErrorCode.INVALID_LOGIN_CREDENTIALS.throwServiceException();
		}
	}

	// 2차 비밀번호 검증
	public void verifySecondaryPassword(String inputPassword) {
		if (!passwordEncoder.matches(inputPassword, systemProperties.getSecondaryPasswordHash())) {
			ErrorCode.SECONDARY_PASSWORD_INVALID.throwServiceException();
		}
	}

	// 2차 비밀번호 미입력
	public void checkSecondaryPasswordPresence(String inputPassword) {
		if (!passwordEncoder.matches(inputPassword, systemProperties.getSecondaryPasswordHash())) {
			ErrorCode.SECONDARY_PASSWORD_REQUIRED.throwServiceException();
		}
	}
}
