package com.ll.quizzle.global.security.oauth2.repository;

import com.ll.quizzle.global.security.oauth2.entity.OAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthRepository extends JpaRepository<OAuth, Long> {
    @Query("SELECT o FROM OAuth o JOIN FETCH o.member WHERE o.provider = :provider AND o.oauthId = :oauthId")
    Optional<OAuth> findByProviderAndOauthIdWithMember(
            @Param("provider") String provider,
            @Param("oauthId") String oauthId
    );

    Optional<OAuth> findByProviderAndOauthId(String provider, String oauthId);

    boolean existsByProviderAndOauthId(String provider, String oauthId);
} 