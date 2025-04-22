package com.ll.quizzle.standard.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.quizzle.global.app.AppConfig;
import com.ll.quizzle.global.jwt.dto.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.SneakyThrows;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class Ut {

    public static class str {
        public static boolean isBlank(String str) {
            return str == null || str.trim().isEmpty();
        }
    }

    public static class json {
        private static final ObjectMapper om = AppConfig.getObjectMapper();

        @SneakyThrows
        public static String toString(Object obj) {
            return om.writeValueAsString(obj);
        }

        @SneakyThrows
        public static Map<String, Object> toMap(String jsonStr) {
            return om.readValue(jsonStr, new TypeReference<Map<String, Object>>() {
            });
        }
    }

    public static class jwt {
        public static String toString(JwtProperties jwtProperties, Map<String, Object> claims) {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
            Date now = new Date();

            String tokenType = (String) claims.getOrDefault("type", "access");
            long expiration = tokenType.equals("refresh")
                    ? jwtProperties.getRefreshTokenExpiration()
                    : jwtProperties.getAccessTokenExpiration(); // 토큰을 타입으로 구분하여 만료 시간 설정

            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)
                    .setExpiration(new Date(now.getTime() + expiration * 1000L))
                    .signWith(secretKey)
                    .compact();
        }

        public static Claims getClaims(JwtProperties jwtProperties, String token) {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
            token = token.replace("Bearer ", "").trim();

            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        }
    }
}
