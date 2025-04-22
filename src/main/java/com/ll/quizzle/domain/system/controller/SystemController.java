package com.ll.quizzle.domain.system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ll.quizzle.domain.system.dto.request.RoleChangeRequest;
import com.ll.quizzle.domain.system.dto.request.SystemLoginRequest;
import com.ll.quizzle.domain.system.dto.response.RoleChangeResponse;
import com.ll.quizzle.domain.system.dto.response.SystemLoginResponse;
import com.ll.quizzle.domain.system.service.SystemService;
import com.ll.quizzle.global.response.RsData;
import com.ll.quizzle.global.security.annotation.RequireSecondaryPassword;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System Controller", description = "최고 관리자 API")
public class SystemController {
	private final SystemService systemService;

	@PostMapping("/login")
	@Operation(summary = "system 계정 로그인", description = "최고 관리자(system)의 로그인 입니다.")
	public RsData<SystemLoginResponse> login(@RequestBody SystemLoginRequest loginRequest) {
		SystemLoginResponse loginResponse = systemService.authenticate(loginRequest);
		return RsData.success(HttpStatus.OK, loginResponse);
	}

	@DeleteMapping("/logout")
	@PreAuthorize("hasRole('SYSTEM')")
	@Operation(summary = "system 계정 로그아웃", description = "system 계정을 로그아웃 합니다.")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		systemService.logout(request, response);
	}

	@PutMapping("/role")
	@PreAuthorize("hasRole('SYSTEM')")
	@Operation(summary = "admin 권한 부여", description = "최고 관리자(system)가 소셜가입된 계정에 admin 권한을 부여합니다.")
	@RequireSecondaryPassword
	public RsData<RoleChangeResponse> updateRole(@RequestBody RoleChangeRequest request) {
		RoleChangeResponse response = systemService.changeRole(request);
		return RsData.success(HttpStatus.OK, response);
	}
}
