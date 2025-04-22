package com.ll.quizzle.domain.member.service;

import com.ll.quizzle.domain.member.dto.response.OAuth2InfoResponse;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.global.request.Rq;
import com.ll.quizzle.global.response.RsData;
import com.ll.quizzle.global.security.oauth2.entity.OAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2Service {
    private final Rq rq;

    public RsData<OAuth2InfoResponse> OAuthInfoToResponseData(String accessToken, String refreshToken, String status) {
        if (!"SUCCESS".equals(status)) {
            return RsData.success(HttpStatus.BAD_REQUEST, new OAuth2InfoResponse(
                    null, null, status, null, null, null,
                    false, false, null, null
            ));
        }

        Member actor = rq.getActor();
        if (actor == null) {
            return RsData.success(HttpStatus.UNAUTHORIZED, new OAuth2InfoResponse(
                    null, null, status, null, null, null,
                    false, false, null, null
            ));
        }

        OAuth oAuth = actor.getOauth();
        if (oAuth == null) {
            return RsData.success(HttpStatus.BAD_REQUEST, new OAuth2InfoResponse(
                    actor.getEmail(), actor.getNickname(), status,
                    actor.getUserRole(), null, null,
                    actor.isMember(), actor.isAdmin(),
                    accessToken, refreshToken
            ));
        }

        OAuth2InfoResponse response = new OAuth2InfoResponse(
                actor.getEmail(),
                actor.getNickname(),
                status,
                actor.getUserRole(),
                oAuth.getProvider(),
                oAuth.getOauthId(),
                actor.isMember(),
                actor.isAdmin(),
                accessToken,
                refreshToken
        );

        return RsData.success(HttpStatus.OK, response);
    }
}
