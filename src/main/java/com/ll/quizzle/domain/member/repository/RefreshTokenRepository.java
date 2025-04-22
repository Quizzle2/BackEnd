package com.ll.quizzle.domain.member.repository;

import com.ll.quizzle.global.jwt.dto.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    Optional<RefreshToken> findByAccessToken(String accessToken);

    boolean existsByRefreshToken(String refreshToken);
}