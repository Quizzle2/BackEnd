package com.ll.quizzle.domain.avatar;

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
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.repository.OwnedAvatarRepository;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.member.service.AuthTokenService;
import com.ll.quizzle.domain.member.type.Role;
import com.ll.quizzle.factory.TestMemberFactory;
import com.ll.quizzle.global.jwt.dto.GeneratedToken;
import com.ll.quizzle.global.security.oauth2.repository.OAuthRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAvatarControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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

	private Cookie adminCookie;
	private Cookie memberCookie;

	@BeforeEach
	void setUp() {
		Member adminMember = TestMemberFactory.createOAuthMember(
			"관리자", "admin@email.com", "google", "1234",
			memberRepository, oAuthRepository, avatarRepository, ownedAvatarRepository
		);
		adminMember.changeRole(Role.ADMIN);
		memberRepository.save(adminMember);

		Member testmember = TestMemberFactory.createOAuthMember(
			"유저2", "user2@email.com", "google", "2345",
			memberRepository, oAuthRepository, avatarRepository, ownedAvatarRepository
		);
		memberRepository.save(testmember);

		GeneratedToken token = authTokenService.generateToken(adminMember.getEmail(), adminMember.getRole().name());
		adminCookie = new Cookie("access_token", token.accessToken());

		GeneratedToken token2 = authTokenService.generateToken(testmember.getEmail(), testmember.getRole().name());
		memberCookie = new Cookie("access_token", token2.accessToken());
	}


	@Test
	@DisplayName("관리자가 아바타 등록에 성공한다.")
	void createAvatar_success() throws Exception {
		Map<String, Object> request = Map.of(
			"fileName", "귀여운아바타.png",
			"url", "https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/cute.png",
			"price", 100
		);

		mockMvc.perform(post("/api/v1/avatars/admin")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(adminCookie)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.msg").value("OK"));
	}

	@Test
	@DisplayName("일반 사용자가 아바타 등록을 시도하면 403 Forbidden이 반환된다.")
	void createAvatar_forbidden_for_user() throws Exception {
		Map<String, Object> request = Map.of(
			"fileName", "불법시도.png",
			"url", "https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/illegal.png",
			"price", 50
		);

		mockMvc.perform(post("/api/v1/avatars/admin")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(memberCookie)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.msg").value("접근 권한이 없습니다."));
	}

	@Test
	@DisplayName("요청 바디가 잘못되었을 경우 400 Bad Request가 반환된다.")
	void createAvatar_invalidRequest() throws Exception {
		Map<String, Object> request = Map.of(  // fileName 누락
			"url", "https://quizzle-avatars.s3.ap-northeast-2.amazonaws.com/missing.png",
			"price", 100
		);

		mockMvc.perform(post("/api/v1/avatars/admin")
				.contentType(MediaType.APPLICATION_JSON)
				.cookie(adminCookie)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

}
