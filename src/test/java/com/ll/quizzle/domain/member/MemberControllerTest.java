package com.ll.quizzle.domain.member;

import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.repository.OwnedAvatarRepository;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.member.service.AuthTokenService;
import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.factory.TestMemberFactory;
import com.ll.quizzle.global.jwt.JwtAuthFilter;
import com.ll.quizzle.global.jwt.dto.GeneratedToken;
import com.ll.quizzle.global.request.Rq;
import com.ll.quizzle.global.security.oauth2.repository.OAuthRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OAuthRepository oAuthRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private OwnedAvatarRepository ownedAvatarRepository;

    @Autowired
    private MemberService memberService;

    @Mock
    private Rq rq;

    private Member member;
    private GeneratedToken generatedTokens;

    @BeforeEach
    void setUp() {
        // ✅ 기본 아바타 포함 테스트 멤버 생성
        member = TestMemberFactory.createOAuthMember(
            "테스트유저",
            "test@email.com",
            "google",
            "1234",
            memberRepository,
            oAuthRepository,
            avatarRepository,
            ownedAvatarRepository
        );

        // ✅ Security MockMvc + 필터 설정
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .addFilter(jwtAuthFilter)
            .apply(springSecurity())
            .alwaysDo(print())
            .build();

        // ✅ 토큰 생성
        generatedTokens = authTokenService.generateToken(
            member.getEmail(),
            member.getRole().toString()
        );

        // ✅ Rq Mock 주입
        ReflectionTestUtils.setField(memberService, "rq", rq);
        when(rq.getActor()).thenReturn(member);

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("프로필 정보 조회 (인증 필요 없음)")
    void testGetProfile() throws Exception {
        mockMvc.perform(get("/api/v1/members/{memberId}", member.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(member.getId()))
            .andExpect(jsonPath("$.data.nickname").value(member.getNickname()));
    }

    @Test
    @DisplayName("프로필 정보 조회 실패 - 없는 회원 (99999999)")
    void testGetProfileWithNoMember() throws Exception {
        mockMvc.perform(get("/api/v1/members/99999999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("내 정보 조회")
    void testGetMyProfile() throws Exception {
        Cookie accessTokenCookie = new Cookie("access_token", generatedTokens.accessToken());
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true);

        mockMvc.perform(get("/api/v1/members/me")
                .cookie(accessTokenCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(member.getId()))
            .andExpect(jsonPath("$.data.nickname").value(member.getNickname()));
    }
}
