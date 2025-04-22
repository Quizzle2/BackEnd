package com.ll.quizzle.domain.system.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.member.type.Role;
import com.ll.quizzle.domain.system.dto.request.RoleChangeRequest;
import com.ll.quizzle.domain.system.dto.request.SystemLoginRequest;
import com.ll.quizzle.domain.system.repository.RoleChangeHistoryRepository;
import com.ll.quizzle.global.config.SystemProperties;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("시스템 ControllerTest")
public class SystemControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SystemProperties systemProperties;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RoleChangeHistoryRepository roleChangeHistoryRepository;

	private static final String SYSTEM_PASSWORD = "system1234";
	private static final String SECONDARY_PASSWORD = "system5678";
	private static final String SYSTEM_EMAIL = "system@quizzle.com";
	private static final String DEFAULT_PROFILE_PATH = "image";

	@BeforeEach
	void setUp() {
		// 기존 데이터 초기화
		memberRepository.deleteAll();
		roleChangeHistoryRepository.deleteAll();

		// SystemProperties 설정
		String encodedSystemPassword = passwordEncoder.encode(SYSTEM_PASSWORD);
		String encodedSecondaryPassword = passwordEncoder.encode(SECONDARY_PASSWORD);

		ReflectionTestUtils.setField(systemProperties, "systemPasswordHash", encodedSystemPassword);
		ReflectionTestUtils.setField(systemProperties, "secondaryPasswordHash", encodedSecondaryPassword);
		ReflectionTestUtils.setField(systemProperties, "systemEmail", SYSTEM_EMAIL);

		// 시스템 계정 생성
		Member systemMember = Member.builder()
			.email(SYSTEM_EMAIL)
			.nickname("System")
			.role(Role.SYSTEM)
			.exp(0)                           // exp 추가
			.level(1)                         // level 추가
			.pointBalance(0)                  // pointBalance 추가
			.build();
		memberRepository.save(systemMember);

		// 관리자 계정 생성
		Member adminMember = Member.builder()
			.email("admin@quizzle.com")
			.nickname("Admin")
			.role(Role.ADMIN)
			.exp(0)
			.level(1)
			.pointBalance(0)
			.build();
		memberRepository.save(adminMember);

		// 일반 회원 계정 생성
		Member normalMember = Member.builder()
			.email("member@quizzle.com")
			.nickname("Member")
			.role(Role.MEMBER)
			.exp(0)
			.level(1)
			.pointBalance(0)
			.build();
		memberRepository.save(normalMember);
	}

	@Test
	@DisplayName("로그인 성공")
	void loginSuccess() throws Exception {
		// Given
		SystemLoginRequest loginRequest = new SystemLoginRequest(
				systemProperties.getSystemEmail(),
				"system1234");

		// When & Then
		mockMvc.perform(post("/api/v1/system/login")
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resultCode").value("OK"))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.role").value("ROLE_SYSTEM"))
			.andExpect(cookie().exists("access_token"))
			.andExpect(cookie().exists("refresh_token"));
	}

	@Test
	@DisplayName("로그인 실패 - 잘못된 이메일")
	void loginFail() throws Exception {
		SystemLoginRequest loginRequest = new SystemLoginRequest(
				"wrongemail@quizzle.com",
				"system1234");

		mockMvc.perform(post("/api/v1/system/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("로그인 실패 - 잘못된 비밀번호")
	void loginFailWithWrongSecondaryPassword() throws Exception {
		SystemLoginRequest loginRequest = new SystemLoginRequest(
				systemProperties.getSystemEmail(),
				"system1111");

		mockMvc.perform(post("/api/v1/system/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"))
			.andExpect(cookie().doesNotExist("access_token"))
			.andExpect(cookie().doesNotExist("refresh_token"));
	}

	@Test
	@DisplayName("권한 변경 성공: member -> admin")
	void changeRoleMemberToAdmin() throws Exception {
		// Given
		SystemLoginRequest loginRequest = new SystemLoginRequest(
			SYSTEM_EMAIL,
			SYSTEM_PASSWORD
		);

		// 로그인 수행하고 쿠키 저장
		MvcResult loginResult = mockMvc.perform(post("/api/v1/system/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isOk())
			.andExpect(cookie().exists("access_token"))
			.andReturn();

		Cookie accessTokenCookie = loginResult.getResponse().getCookie("access_token");

		String memberEmail = "member@quizzle.com";
		RoleChangeRequest request = new RoleChangeRequest(
			memberEmail,
			Role.ADMIN,
			"관리자 권한 부여",
			SECONDARY_PASSWORD,
			SYSTEM_EMAIL
		);

		// When & Then
		mockMvc.perform(put("/api/v1/system/role")
				.cookie(accessTokenCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resultCode").value("OK"))
			.andExpect(jsonPath("$.data.targetEmail").value(memberEmail))
			.andExpect(jsonPath("$.data.previousRole").value("MEMBER"))
			.andExpect(jsonPath("$.data.newRole").value("ADMIN"));

		// DB 검증
		Member updatedMember = memberRepository.findByEmail(memberEmail).orElseThrow();
		assertThat(updatedMember.getRole()).isEqualTo(Role.ADMIN);
	}
}
