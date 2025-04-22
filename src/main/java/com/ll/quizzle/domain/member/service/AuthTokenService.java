package com.ll.quizzle.domain.member.service;

import com.ll.quizzle.global.jwt.dto.GeneratedToken;
import com.ll.quizzle.global.jwt.dto.JwtProperties;
import com.ll.quizzle.standard.util.Ut;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthTokenService {
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private static final int EXPIRATION_TIME = 5 * 60 * 1000;

    public GeneratedToken generateToken(String email, String role) {
        String accessToken = genAccessToken(email, role);
        String refreshToken = refreshTokenService.generateRefreshToken(email);

        refreshTokenService.saveTokenInfo(email, refreshToken, accessToken);
        return new GeneratedToken(accessToken, refreshToken);
    }

    String genAccessToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", email);
        claims.put("role", role);
        claims.put("type", "access");

        return Ut.jwt.toString(jwtProperties, claims);
    }

    boolean verifyToken(String token) {
        try {
            Claims claims = Ut.jwt.getClaims(jwtProperties, token);
            return claims.getExpiration().after(new Date());
        } catch (ExpiredJwtException e) {
            log.debug("Access Token 만료됨: {}", e.getMessage());
        } catch (SignatureException e) {
            log.debug("Access Token 서명 불일치: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("Access Token 구조 이상: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("Access Token 검증 실패: {}", e.getMessage());
        }
        return false;
    }

    String getEmail(String token) {
        return Ut.jwt.getClaims(jwtProperties, token).getSubject();
    }


    public Long getTokenExpiryTime(String token) {
        if (token == null || token.isEmpty()) {
            return System.currentTimeMillis() + EXPIRATION_TIME;
        }

        try {
            Claims claims = Ut.jwt.getClaims(jwtProperties, token);
            if (claims.getExpiration() != null) {
                long expiryTime = claims.getExpiration().getTime();
                log.debug("토큰 만료 시간 추출 (밀리초): {}, Date: {}", expiryTime, claims.getExpiration());
                return expiryTime;
            }
        } catch (ExpiredJwtException e) {
            long expiryTime = e.getClaims().getExpiration().getTime();
            log.debug("만료된 토큰의 만료 시간 추출 (밀리초): {}, Date: {}", expiryTime, e.getClaims().getExpiration());
            return expiryTime;
        } catch (SignatureException | MalformedJwtException e) {
            log.error("잘못된 JWT 토큰: {}", e.getMessage());
        } catch (Exception e) {
            log.error("토큰에서 만료 시간 추출 중 오류: {}", e.getMessage());
        }

        return System.currentTimeMillis() + EXPIRATION_TIME;
    }
}