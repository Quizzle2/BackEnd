package com.ll.quizzle.domain.member;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.repository.OwnedAvatarRepository;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.member.service.AuthTokenService;
import com.ll.quizzle.factory.TestMemberFactory;
import com.ll.quizzle.global.jwt.dto.GeneratedToken;
import com.ll.quizzle.global.security.oauth2.repository.OAuthRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberNicknameEditTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private OAuthRepository oauthRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private OwnedAvatarRepository ownedAvatarRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member member;
    private Cookie accessTokenCookie;
    private Avatar defaultAvatar;


    @BeforeEach
    void setUp() {
        member = TestMemberFactory.createOAuthMember(
            "테스트유저", "test@email.com", "google", "1234",
            memberRepository, oauthRepository, avatarRepository, ownedAvatarRepository
        );

        member.increasePoint(100); // 초기 포인트 설정
        memberRepository.save(member);

        GeneratedToken token = authTokenService.generateToken(member.getEmail(), member.getRole().name());
        accessTokenCookie = new Cookie("access_token", token.accessToken());
    }


    @Test
    @DisplayName("닉네임 변경 성공")
    void updateNickname_success() throws Exception {
        String newNickname = "변경된닉네임";
        mockMvc.perform(patch("/api/v1/members/" + member.getId() + "/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(accessTokenCookie)
                .content(objectMapper.writeValueAsString(Map.of("nickname", newNickname))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.profile").value(newNickname));
    }

    @Test
    @DisplayName("포인트가 없을 때 닉네임 변경 불가")
    void updateNickname_noPoints() throws Exception {
        // 포인트를 0으로 설정
        member.decreasePoint(100);
        memberRepository.save(member);

        mockMvc.perform(patch("/api/v1/members/" + member.getId() + "/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(accessTokenCookie)
                .content(objectMapper.writeValueAsString(Map.of("nickname", "새닉네임"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.msg").value("포인트가 부족합니다."));
    }

    @Test
    @DisplayName("닉네임이 공백일 때")
    void updateNickname_empty() throws Exception {
        mockMvc.perform(patch("/api/v1/members/" + member.getId() + "/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(accessTokenCookie)
                .content(objectMapper.writeValueAsString(Map.of("nickname", " "))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("닉네임이 유효하지 않습니다.")));
    }

    @Test
    @DisplayName("닉네임이 너무 짧을 때")
    void updateNickname_tooShort() throws Exception {
        mockMvc.perform(patch("/api/v1/members/" + member.getId() + "/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(accessTokenCookie)
                .content(objectMapper.writeValueAsString(Map.of("nickname", "A"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("닉네임은 2자 이상 20자 이하이어야 합니다.")));
    }

    @Test
    @DisplayName("닉네임에 특수문자가 포함될 때")
    void updateNickname_invalidFormat() throws Exception {
        mockMvc.perform(patch("/api/v1/members/" + member.getId() + "/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(accessTokenCookie)
                .content(objectMapper.writeValueAsString(Map.of("nickname", "Invalid@Nick"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("닉네임은 영문, 숫자, 한글만 사용할 수 있습니다.")));

    }

    @Test
    @DisplayName("중복된 닉네임일 때")
    void updateNickname_duplicate() throws Exception {
        // 중복 닉네임을 가진 사용자 생성 (기본 아바타 소유 포함)
        TestMemberFactory.createOAuthMember(
            "중복닉네임",
            "duplicate@email.com",
            "google",
            "5678",
            memberRepository,
            oauthRepository,
            avatarRepository,
            ownedAvatarRepository
        );

        mockMvc.perform(patch("/api/v1/members/" + member.getId() + "/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(accessTokenCookie)
                .content(objectMapper.writeValueAsString(Map.of("nickname", "중복닉네임"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.msg").value("이미 존재하는 닉네임입니다."));
    }

}
