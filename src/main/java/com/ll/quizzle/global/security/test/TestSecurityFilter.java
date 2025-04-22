package com.ll.quizzle.global.security.test;

import com.ll.quizzle.global.security.oauth2.dto.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Profile("test")
@Component
@RequiredArgsConstructor
public class TestSecurityFilter extends OncePerRequestFilter {

    // 테스트용 SecurityFilter
    // 테스트 환경에서만 동작하도록 @Profile("test") 어노테이션을 사용
    // 테스트 클래스에 @ActiveProfiles("test") 어노테이션을 추가

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 이미 인증이 되어 있으면 생략
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            // 테스트용 SecurityUser 생성
            SecurityUser testUser = SecurityUser.of(
                    999L,
                    "TestUser",
                    "test@example.com",
                    "ROLE_MEMBER"
            );

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    testUser,
                    null,
                    testUser.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
