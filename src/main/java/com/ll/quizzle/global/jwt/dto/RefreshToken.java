package com.ll.quizzle.global.jwt.dto;

import jakarta.persistence.Id;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@Getter
@RedisHash(value = "rt")
public class RefreshToken implements Serializable {

    @Id
    private String id;

    @Indexed
    private String refreshToken;

    private String accessToken;

    @TimeToLive
    private Long timeToLive = 86400L;

    // 기본 생성자 추가
    protected RefreshToken() {
    }

    // 모든 필드를 포함하는 생성자
    public RefreshToken(String id, String refreshToken, String accessToken) {
        this.id = id;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
    }

    public void updateAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("액세스 토큰은 null 이거나 비어 있을 수 없습니다");
        }
        this.accessToken = accessToken;
    }

}
