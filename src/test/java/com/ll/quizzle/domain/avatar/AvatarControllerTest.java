package com.ll.quizzle.domain.avatar;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.entity.OwnedAvatar;
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.repository.OwnedAvatarRepository;
import com.ll.quizzle.domain.avatar.service.AvatarService;
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
class AvatarControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AvatarRepository avatarRepository;

	@Autowired
	private OwnedAvatarRepository ownedAvatarRepository;

	@Autowired
	private AvatarService avatarService;

	@Autowired
	private OAuthRepository oAuthRepository;

	@Autowired
	private AuthTokenService authTokenService;

	private Member member;
	private Cookie accessTokenCookie;
	private Avatar availableAvatar;
	private Avatar defaultAvatar;


	@BeforeEach
	void setUp() {
		defaultAvatar = avatarRepository.findByFileName("새콩이")
			.orElseThrow(AVATAR_NOT_FOUND::throwServiceException);

		member = TestMemberFactory.createOAuthMember(
			"구매자", "buyer@email.com", "google", "1234",
			memberRepository, oAuthRepository, avatarRepository, ownedAvatarRepository
		);
		member.increasePoint(1000);
		memberRepository.save(member);

		availableAvatar = avatarRepository.findByFileName("안경쓴 새콩이")
			.orElseThrow(AVATAR_NOT_FOUND::throwServiceException);

		GeneratedToken token = authTokenService.generateToken(member.getEmail(), member.getRole().name());
		accessTokenCookie = new Cookie("access_token", token.accessToken());
	}

	@Test
	@DisplayName("아바타 구매 성공")
	void purchaseAvatar_success() throws Exception {
		mockMvc.perform(post("/api/v1/members/" + member.getId() + "/avatars/" + availableAvatar.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(accessTokenCookie))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("이미 소유한 아바타 구매 시도 시 실패")
	void purchaseAvatar_alreadyOwned() throws Exception {
		OwnedAvatar owned = OwnedAvatar.create(member, availableAvatar);
		member.addOwnedAvatar(owned);
		ownedAvatarRepository.save(owned);
		memberRepository.save(member);

		// 동일 아바타 재구매 시도 → 실패 예상
		mockMvc.perform(post("/api/v1/members/" + member.getId() + "/avatars/" + availableAvatar.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(accessTokenCookie))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.msg").value("이미 소유한 아바타입니다."));
	}


	@Test
	@DisplayName("보유 포인트가 부족한 경우 실패")
	void purchaseAvatar_insufficientPoints() throws Exception {
		member.decreasePoint(member.getPointBalance());
		memberRepository.save(member);

		mockMvc.perform(post("/api/v1/members/" + member.getId() + "/avatars/" + availableAvatar.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(accessTokenCookie))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.msg").value("포인트가 부족합니다."));
	}

	@Test
	@DisplayName("소유한 아바타 목록 조회 성공")
	void getOwnedAvatars_success() throws Exception {

		OwnedAvatar owned = OwnedAvatar.create(member, availableAvatar);
		member.addOwnedAvatar(owned);
		ownedAvatarRepository.save(owned);
		memberRepository.save(member);

		mockMvc.perform(get("/api/v1/members/" + member.getId() + "/avatars/owned")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(accessTokenCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[*].id").value(org.hamcrest.Matchers.containsInAnyOrder(
				defaultAvatar.getId().intValue(), availableAvatar.getId().intValue()
			)));
	}



	@Test
	@DisplayName("구매 가능한 아바타 목록 조회 성공")
	void getAvailableAvatars_success() throws Exception {
		mockMvc.perform(get("/api/v1/members/" + member.getId() + "/avatars/available")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(accessTokenCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());
	}
}
