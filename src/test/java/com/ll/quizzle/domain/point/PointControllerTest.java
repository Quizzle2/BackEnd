package com.ll.quizzle.domain.point;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.repository.OwnedAvatarRepository;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.member.service.AuthTokenService;
import com.ll.quizzle.domain.point.repository.PointRepository;
import com.ll.quizzle.factory.PointTestFactory;
import com.ll.quizzle.factory.TestMemberFactory;
import com.ll.quizzle.global.jwt.dto.GeneratedToken;
import com.ll.quizzle.global.security.oauth2.repository.OAuthRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PointControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PointRepository pointRepository;

	@Autowired
	private AuthTokenService authTokenService;

	@Autowired
	private OAuthRepository oAuthRepository;

	@Autowired
	private AvatarRepository avatarRepository;

	@Autowired
	private OwnedAvatarRepository ownedAvatarRepository;

	private Member member;
	private Member other;
	private Cookie accessTokenCookie;

	@BeforeEach
	void setUp() {
		// 테스트 유저 생성 (기본 아바타 장착 및 소유 포함)
		member = TestMemberFactory.createOAuthMember(
			"테스트유저", "test@email.com", "google", "1234",
			memberRepository, oAuthRepository, avatarRepository, ownedAvatarRepository
		);

		// 다른 유저도 동일하게 처리
		other = TestMemberFactory.createOAuthMember(
			"다른유저", "other@email.com", "google", "5678",
			memberRepository, oAuthRepository, avatarRepository, ownedAvatarRepository
		);

		// 포인트 내역 생성
		PointTestFactory.createRewardPoint(member, pointRepository, 100);
		PointTestFactory.createUsePoint(member, pointRepository, 30);

		// 인증 토큰 생성 및 쿠키 설정
		GeneratedToken token = authTokenService.generateToken(member.getEmail(), member.getRole().name());
		accessTokenCookie = new Cookie("access_token", token.accessToken());
	}


	@Test
	@DisplayName("포인트 내역 전체 조회 성공")
	void getPoints_success() throws Exception {
		mockMvc.perform(get("/api/v1/members/" + member.getId() + "/points")
				.cookie(accessTokenCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items").isArray())
			.andExpect(jsonPath("$.data.items.length()").value(2));
	}

	@Test
	@DisplayName("REWARD 타입 필터링 조회")
	void getPoints_rewardOnly() throws Exception {
		mockMvc.perform(get("/api/v1/members/" + member.getId() + "/points")
				.param("type", "REWARD")
				.cookie(accessTokenCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items.length()").value(1))
			.andExpect(jsonPath("$.data.items[0].type").value("REWARD"));
	}

	@Test
	@DisplayName("USE 타입 필터링 조회")
	void getPoints_useOnly() throws Exception {
		mockMvc.perform(get("/api/v1/members/" + member.getId() + "/points")
				.param("type", "USE")
				.cookie(accessTokenCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items.length()").value(1))
			.andExpect(jsonPath("$.data.items[0].type").value("USE"));
	}

	@Test
	@DisplayName("인증되지 않은 사용자 → 401")
	void getPoints_unauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/members/" + member.getId() + "/points"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("다른 유저의 포인트 내역 조회 시 → 403")
	void getPoints_forbidden() throws Exception {
		mockMvc.perform(get("/api/v1/members/" + other.getId() + "/points")
				.cookie(accessTokenCookie))
			.andExpect(status().isForbidden());
	}
}
