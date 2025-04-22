package com.ll.quizzle.global.security.aop;

import java.util.stream.Stream;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.ll.quizzle.domain.system.dto.request.RoleChangeRequest;
import com.ll.quizzle.domain.system.service.SystemVerifier;
import com.ll.quizzle.global.config.SystemProperties;
import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.security.annotation.RequireSecondaryPassword;
import com.ll.quizzle.global.security.oauth2.dto.SecurityUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecondaryPasswordAspect {
	private final SystemProperties systemProperties;
	private final BCryptPasswordEncoder passwordEncoder;
	private final SystemVerifier systemVerifier;

	// @RequireSecondaryPassword 라는 커스텀 어노테이션이 붙은 메서드 실행 전후에 2차 비밀번호가 입력되었는지 확인
	@Around("@annotation(requireSecondaryPassword)")
	public Object validateSecondaryPassword(ProceedingJoinPoint joinPoint,
		RequireSecondaryPassword requireSecondaryPassword) throws Throwable {

		String secondaryPassword = extractSecondaryPasswordFromRequest(joinPoint);

		// 2차 비밀번호를 입력하지 않았다면 예외 처리
		systemVerifier.checkSecondaryPasswordPresence(secondaryPassword);

		SecurityUser user = getCurrentUser();
		if (isSystem(user)) {
			systemVerifier.verifySecondaryPassword(secondaryPassword);
		}

		return joinPoint.proceed();
	}

	// 현재 로그인된 사용자를 SecurityContext 에서 꺼내옴
	private SecurityUser getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof SecurityUser)) {
			ErrorCode.UNAUTHORIZED.throwServiceException();
		}
		return (SecurityUser) auth.getPrincipal();
	}

	// System 여부 확인
	private boolean isSystem(SecurityUser user) {
		return systemProperties.getSystemEmail().equals(user.getEmail());
	}

	// 요청 객체애서 2차 비밀번호 값을 추출하는 매서드
	private String extractSecondaryPasswordFromRequest(ProceedingJoinPoint joinPoint) {
		return Stream.of(joinPoint.getArgs())
			.filter(arg -> arg instanceof RoleChangeRequest)
			.map(arg -> ((RoleChangeRequest) arg).secondaryPassword())
			.findFirst()
			.orElse(null);
	}
}
