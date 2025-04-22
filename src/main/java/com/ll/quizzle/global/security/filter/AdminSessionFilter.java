package com.ll.quizzle.global.security.filter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.exceptions.ServiceException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSessionFilter extends OncePerRequestFilter {

	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String requestURI = request.getRequestURI();

		try {
			if (requestURI.startsWith("/api/v1/admin") && !requestURI.equals("/api/v1/admin/login")) {
				HttpSession session = request.getSession(false);

				if (session == null || session.getAttribute("ADMIN_KEY") == null) {
					throw ErrorCode.UNAUTHORIZED.throwServiceException();
				}

				// 세션에서 관리자 정보 확인
				String adminEmail = (String) session.getAttribute("ADMIN_KEY");
				String adminRole = (String) session.getAttribute("ADMIN_ROLE");

				if (adminRole == null || !adminRole.equals("ROLE_ADMIN")) {
					throw ErrorCode.FORBIDDEN_ACCESS.throwServiceException();
				}

				// Security Context에 인증 정보 설정
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					adminEmail,
					null,
					Collections.singletonList(new SimpleGrantedAuthority(adminRole))
				);

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}

			filterChain.doFilter(request, response);
		} catch (ServiceException e) {
			response.setContentType("application/json;charset=UTF-8");
			response.setStatus(e.getHttpStatus().value());
			response.getWriter().write(
				objectMapper.writeValueAsString(
					new ErrorResponse(e.getMsg())
				)
			);
		}
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.equals("/api/v1/admin/login");
	}

	// 에러 응답용 내부 클래스
	private static class ErrorResponse {
		private final String message;

		public ErrorResponse(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}
	}
}
