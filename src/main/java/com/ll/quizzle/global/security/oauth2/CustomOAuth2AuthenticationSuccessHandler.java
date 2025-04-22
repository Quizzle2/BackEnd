package com.ll.quizzle.global.security.oauth2;


import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.service.AuthTokenService;
import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.global.jwt.dto.JwtProperties;
import com.ll.quizzle.global.security.oauth2.dto.SecurityUser;
import com.ll.quizzle.standard.util.CookieUtil;
import com.ll.quizzle.standard.util.Ut;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthTokenService authTokenService;
    private final MemberService memberService;
    private final JwtProperties jwtProperties;

    @Value("${app.oauth2.authorizedRedirectUris}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        log.debug("OAuth2 login success - provider: {}, oauthId: {}, email: {}",
                securityUser.getProvider(), securityUser.getOauthId(), securityUser.getEmail());

        Member member = memberService.findByProviderAndOauthId(
                securityUser.getProvider(),
                securityUser.getOauthId()
        );

        String accessToken = authTokenService.generateToken(member.getEmail(), member.getUserRole()).accessToken();
        String refreshToken = memberService.generateRefreshToken(member.getEmail());
        log.debug("Generated JWT access token: {}", accessToken);
        log.debug("Generated JWT refresh token: {}", refreshToken);


        Map<String, Object> roleData = new HashMap<>();
        roleData.put("role", member.getUserRole());
        String encodedRoleData = URLEncoder.encode(Ut.json.toString(roleData), StandardCharsets.UTF_8);

        CookieUtil.addCookie(
                response,
                "role",
                encodedRoleData,
                (int) jwtProperties.getAccessTokenExpiration(),
                false, //httpOnly
                true //secure
        );

        CookieUtil.addCookie(
                response,
                "access_token",
                accessToken,
                (int) jwtProperties.getAccessTokenExpiration(),
                true, //httpOnly
                true //secure
        );

        CookieUtil.addCookie(
                response,
                "refresh_token",
                refreshToken,
                (int) jwtProperties.getRefreshTokenExpiration(),
                true, //httpOnly
                true //secure
        );

        String status = securityUser.isNewUser() ? "REGISTER" : "SUCCESS";
        String redirectUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                .queryParam("status", status)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}