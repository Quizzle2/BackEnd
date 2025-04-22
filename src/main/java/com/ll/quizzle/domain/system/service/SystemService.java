package com.ll.quizzle.domain.system.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.member.service.AuthTokenService;
import com.ll.quizzle.domain.member.service.RefreshTokenService;
import com.ll.quizzle.domain.member.type.Role;
import com.ll.quizzle.domain.system.dto.request.RoleChangeRequest;
import com.ll.quizzle.domain.system.dto.request.SystemLoginRequest;
import com.ll.quizzle.domain.system.dto.response.RoleChangeResponse;
import com.ll.quizzle.domain.system.dto.response.SystemLoginResponse;
import com.ll.quizzle.domain.system.entity.RoleChangeHistory;
import com.ll.quizzle.domain.system.repository.RoleChangeHistoryRepository;
import com.ll.quizzle.global.config.SystemProperties;
import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.jwt.dto.GeneratedToken;
import com.ll.quizzle.global.jwt.dto.JwtProperties;
import com.ll.quizzle.standard.util.CookieUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemService {
	private final SystemProperties systemProperties;
	private final AuthTokenService authTokenService;
	private final JwtProperties jwtProperties;
	private final MemberRepository memberRepository;
	private final RoleChangeHistoryRepository roleChangeHistoryRepository;
	private final RefreshTokenService refreshTokenService;
	private final SystemVerifier passwordVerifier;

	public SystemLoginResponse authenticate(SystemLoginRequest loginRequest) {
		passwordVerifier.verifySystemEmail(loginRequest.systemEmail());
		passwordVerifier.verifySystemPassword(loginRequest.systemPassword());

		GeneratedToken tokenInfo = authTokenService.generateToken(loginRequest.systemEmail(), "ROLE_SYSTEM");

		addAuthCookies(tokenInfo);

		return new SystemLoginResponse(
			tokenInfo.accessToken(),
			tokenInfo.refreshToken(),
			"ROLE_SYSTEM"
		);
	}

	@Transactional
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		try {
			String refreshToken = null;
			Cookie[] cookies = request.getCookies();

			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if ("refresh_token".equals(cookie.getName())) {
						refreshToken = cookie.getValue();
						break;
					}
				}
			}

			if (refreshToken != null) {
				refreshTokenService.removeRefreshToken(refreshToken);
			}

			CookieUtil.deleteCookie(request, response, "access_token");
			CookieUtil.deleteCookie(request, response, "refresh_token");

		} catch (Exception e) {
			log.error("로그아웃 처리 중 오류 발생", e);
			throw e;
		}
	}

	@Transactional
	public RoleChangeResponse changeRole(RoleChangeRequest request) {
		passwordVerifier.verifySecondaryPassword(request.secondaryPassword());

		Member member = memberRepository.findByEmail(request.targetEmail())
			.orElseThrow(ErrorCode.MEMBER_NOT_FOUND::throwServiceException);

		Role previousRole = member.getRole();

		member.changeRole(request.newRole());

		recordRoleChange(member, previousRole, request);

		return new RoleChangeResponse(
			member.getEmail(),
			previousRole,
			request.newRole(),
			request.reason()
		);
	}

	private void addAuthCookies(GeneratedToken tokens) {
		HttpServletResponse response = (
			(ServletRequestAttributes)RequestContextHolder.currentRequestAttributes())
			.getResponse();

		if (response == null) {
			log.error("HttpServletResponse is null. 쿠키를 설정할 수 없습니다.");
			return;
		}

		try {
			CookieUtil.addSystemCookie(
				response,
				"access_token",
				tokens.accessToken(),
				(int)jwtProperties.getAccessTokenExpiration()
			);

			CookieUtil.addSystemCookie(
				response,
				"refresh_token",
				tokens.refreshToken(),
				(int)jwtProperties.getRefreshTokenExpiration()
			);

		} catch (Exception e) {
			log.error("쿠키 설정 중 오류 발생: {}", e.getMessage());
		}
	}

	private void recordRoleChange(
		Member member,
		Role previousRole,
		RoleChangeRequest request
	) {
		RoleChangeHistory history = RoleChangeHistory.builder()
			.member(member)
			.previousRole(previousRole)
			.newRole(request.newRole())
			.reason(request.reason())
			.changedBy(systemProperties.getSystemEmail())
			.build();

		roleChangeHistoryRepository.save(history);
	}
}
