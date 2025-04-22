package com.ll.quizzle.global.jwt;


import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.global.exceptions.ServiceException;
import com.ll.quizzle.global.response.RsData;
import com.ll.quizzle.global.security.oauth2.dto.SecurityUser;
import com.ll.quizzle.standard.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.ll.quizzle.global.exceptions.ErrorCode.MEMBER_NOT_FOUND;
import static com.ll.quizzle.global.exceptions.ErrorCode.TOKEN_EXPIRED;
import static com.ll.quizzle.global.security.oauth2.dto.SecurityUser.of;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter implements Ordered {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        String accessToken = null;
        String refreshToken = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    log.debug("Access Token 존재: {}", accessToken);
                } else if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    log.debug("Refresh Token 존재: {}", refreshToken);
                }
            }
        }
        log.debug("쿠키 액세스토큰 : {}", accessToken);
        log.debug("쿠키 리프레시토큰 : {}", refreshToken);

        if (StringUtils.hasText(accessToken)) {
            try {
                String email = memberService.extractEmailIfValid(accessToken);
                log.debug("Access Token 유효함. Email: {}", email);

                Member member = memberRepository.findByEmail(email)
                        .orElseThrow(MEMBER_NOT_FOUND::throwServiceException);
                log.debug("Found member: {}", member.getEmail());

                SecurityUser userDto = of(
                        member.getId(),
                        member.getNickname(),
                        member.getEmail(),
                        "ROLE_" + member.getRole()
                );

                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userDto,
                        null,
                        userDto.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (ServiceException e) {
                String errorMessage = e.getMessage();
                String resultCode = errorMessage.split(" : ")[0];
                log.debug("Token 검증 실패. Error Code: {}, Message: {}", resultCode, errorMessage);

                if (resultCode.equals("401-2") && StringUtils.hasText(refreshToken)) {
                    log.debug("Access Token 만료됨. Refresh Token으로 갱신 시도");
                    try {
                        RsData<String> refreshResult = memberService.refreshAccessToken(refreshToken);
                        if (refreshResult.isSuccess()) {
                            log.debug("새로운 Access Token 발급 성공");
                            CookieUtil.addCookie(
                                    response,
                                    "access_token",
                                    refreshResult.getData(),
                                    86400,
                                    true,
                                    true
                            );

                            String email = memberService.extractEmailIfValid(refreshResult.getData());
                            Member member = memberRepository.findByEmail(email)
                                    .orElseThrow(MEMBER_NOT_FOUND::throwServiceException);

                            SecurityUser userDto = of(
                                    member.getId(),
                                    member.getNickname(),
                                    member.getEmail(),
                                    "ROLE_" + member.getRole()
                            );

                            Authentication auth = new UsernamePasswordAuthenticationToken(
                                    userDto,
                                    null,
                                    userDto.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    } catch (Exception refreshError) {
                        TOKEN_EXPIRED.throwServiceException();
                    }
                } else {
                    throw e;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String requestURI = request.getRequestURI();

        return requestURI.equals("/api/v1/members/refresh") ||
//                (requestURI.contains("/oauth2/callback") && request.getParameter("accessToken") == null) ||
                requestURI.startsWith("/h2-console");
    }
}
