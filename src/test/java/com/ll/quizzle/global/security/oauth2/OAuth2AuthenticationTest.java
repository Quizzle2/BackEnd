package com.ll.quizzle.global.security.oauth2;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.type.AvatarStatus;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.domain.member.type.Role;
import com.ll.quizzle.global.request.Rq;
import com.ll.quizzle.global.security.oauth2.entity.OAuth;
import com.ll.quizzle.global.security.oauth2.repository.OAuthRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public class OAuth2AuthenticationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OAuthRepository oAuthRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private Rq rq;

    private Member testMember;
    private OAuth testOAuth;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        response = new MockHttpServletResponse();

        // Rq 모킹 설정
        ReflectionTestUtils.setField(memberService, "rq", rq);

        Avatar defaultAvatar = avatarRepository.save(Avatar.builder()
            .fileName("새콩이")
            .url("https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/%EA%B8%B0%EB%B3%B8+%EC%95%84%EB%B0%94%ED%83%80.png")
            .price(0)
            .build());

        testMember = Member.builder()
            .nickname("OAuth2 테스트")
            .email("oauth2test@example.com")
            .level(0)
            .role(Role.MEMBER)
            .exp(0)
            .pointBalance(0)
            .avatar(defaultAvatar)
            .build();

        memberRepository.save(testMember);

        // 테스트용 OAuth 정보 생성
        testOAuth = OAuth.builder()
            .provider("google")
            .oauthId("123456789")
            .member(testMember)
            .build();

        oAuthRepository.save(testOAuth);
        memberRepository.save(testMember);

        when(rq.getActor()).thenReturn(testMember);
    }

    @Test
    @DisplayName("OAuth2 로그인 후 토큰 검증")
    void validateTokenAfterOAuth2Login() {
        // given
        // OAuth2 로그인 프로세스
        memberService.oAuth2Login(testMember, response);

        // 토큰 추출
        String accessToken = response.getCookie("access_token").getValue();
        String refreshToken = response.getCookie("refresh_token").getValue();

        // when & then
        // 액세스 토큰 검증
        assertThat(memberService.verifyToken(accessToken)).isTrue();
        // 리프레시 토큰 검증
        assertThat(memberService.verifyToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("OAuth 쿠키 기반 인증 테스트")
    void cookieBasedAuthenticationTest() throws Exception {
        // given
        memberService.oAuth2Login(testMember, response);
        Cookie accessTokenCookie = response.getCookie("access_token");

        // when & then
        mockMvc.perform(get("/api/v1/members/{memberId}/points", testMember.getId())
                .cookie(accessTokenCookie))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OAuth 인증 후 SecurityContext 설정 테스트")
    void securityContextAfterOAuth2Login() {
        // given
        // 인증 정보 생성
        Authentication auth = new UsernamePasswordAuthenticationToken(
            testMember,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        // when
        SecurityContextHolder.getContext().setAuthentication(auth);

        // then
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(currentAuth).isNotNull();
        assertThat(currentAuth.getPrincipal()).isEqualTo(testMember);
    }

    @Test
    @DisplayName("OAuth 제공자로 회원 조회 테스트")
    void findMemberByProviderAndOauthId() {
        // given
        String provider = "google";
        String oauthId = "123456789";

        // when
        Member foundMember = memberService.findByProviderAndOauthId(provider, oauthId);

        // then
        assertThat(foundMember).isNotNull();
        assertThat(foundMember.getId()).isEqualTo(testMember.getId());
        assertThat(foundMember.getEmail()).isEqualTo(testMember.getEmail());
    }

    @Test
    @DisplayName("OAuth 로그인 실패 - 유효하지 않은 제공자")
    void oauthLoginFail_InvalidProvider() {
        // given
        String invalidProvider = "invalid-provider";
        String oauthId = "123456789";

        // when & then
        assertThat(oAuthRepository.findByProviderAndOauthIdWithMember(invalidProvider, oauthId))
            .isEmpty();
    }
}