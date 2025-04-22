package com.ll.quizzle.domain.member;

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
import com.ll.quizzle.domain.avatar.entity.OwnedAvatar;
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
class MemberAvatarEditTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AvatarRepository avatarRepository;

	@Autowired
	private OwnedAvatarRepository ownedAvatarRepository;

	@Autowired
	private OAuthRepository oAuthRepository;

	@Autowired
	private AuthTokenService authTokenService;

	private Member member;
	private Avatar ownedAvatar;
	private Avatar notOwnedAvatar;
	private Cookie accessTokenCookie;

	@BeforeEach
	void setUp() {
		// Member 생성 및 기본 아바타 소유 처리
		member = TestMemberFactory.createOAuthMember(
			"바이어", "buyer@email.com", "google", "7777",
			memberRepository, oAuthRepository, avatarRepository, ownedAvatarRepository
		);

		ownedAvatar = avatarRepository.save(
			Avatar.builder()
				.fileName("소유한 아바타")
				.url("https://url.com/owned.png")
				.price(0)
				.build()
		);

		member.addOwnedAvatar(OwnedAvatar.create(member, ownedAvatar));
		ownedAvatarRepository.save(OwnedAvatar.create(member, ownedAvatar));

		notOwnedAvatar = avatarRepository.save(
			Avatar.builder()
				.fileName("소유하지 않은 아바타")
				.url("https://url.com/notowned.png")
				.price(100)
				.build()
		);

		GeneratedToken token = authTokenService.generateToken(member.getEmail(), member.getRole().name());
		accessTokenCookie = new Cookie("access_token", token.accessToken());
	}

	@Test
	@DisplayName("소유한 아바타로 아바타 변경 성공")
	void editAvatar_success() throws Exception {
		mockMvc.perform(patch("/api/v1/members/" + member.getId() + "/avatars/" + ownedAvatar.getId())
				.cookie(accessTokenCookie))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("소유하지 않은 아바타로 변경 시도 시 예외 발생")
	void editAvatar_notOwned() throws Exception {
		mockMvc.perform(patch("/api/v1/members/" + member.getId() + "/avatars/" + notOwnedAvatar.getId())
				.cookie(accessTokenCookie))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.msg").value(AVATAR_NOT_OWNED.getMessage()));
	}

	@Test
	@DisplayName("이미 적용된 아바타로 다시 적용 시도 시 예외 발생")
	void editAvatar_alreadyApplied() throws Exception {
		// 아바타를 먼저 적용
		member.changeAvatar(ownedAvatar);
		memberRepository.save(member);

		mockMvc.perform(patch("/api/v1/members/" + member.getId() + "/avatars/" + ownedAvatar.getId())
				.cookie(accessTokenCookie))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.msg").value(AVATAR_ALREADY_APPLIED.getMessage()));
	}
}
