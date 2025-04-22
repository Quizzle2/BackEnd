package com.ll.quizzle.domain.member.controller;

import com.ll.quizzle.domain.member.dto.response.OAuth2InfoResponse;
import com.ll.quizzle.domain.member.service.OAuth2Service;
import com.ll.quizzle.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j
@Tag(name = "OAuth2Controller", description = "OAuth2 관련 API")
public class OAuth2Controller {
    private final OAuth2Service oAuth2Service;


    /**
     * OAuth 정보 확인용 (테스트를 위해 일단 보류)
     *
     * @param accessToken  the access token
     * @param refreshToken the refresh token
     * @param status       the status (SUCCESS)
     *
     * @return OAuth2InfoResponse
     */
    @GetMapping("/oauth2/callback")
    @Operation(summary = "oauth 정보 확인용", description = "OAuth2 인증 정보 확인 API입니다. Status에 SUCCESS가 아닌 경우, 인증 실패로 간주합니다.")
    public RsData<OAuth2InfoResponse> callback(
            @CookieValue(value = "access_token", required = false) String accessToken,
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            @RequestParam String status
    ) {
        return oAuth2Service.OAuthInfoToResponseData(accessToken, refreshToken, status);
    }

    @GetMapping("/no-use")
    @Operation(summary = "소셜로그인 방법 (문서화를 위한, 사용하지 않는 api입니다.)", description = """
            localhost:8080/oauth2/authorization/{provider} \n
            provider : kakao, google \n
            카카오, 구글 로그인 주소입니다.
            """)
    public void noUse() {
        // 이 API는 사용되지 않음
        // 실제로는 사용되지 않지만, Swagger 문서화 용도로 남겨둡니다.
    }
}