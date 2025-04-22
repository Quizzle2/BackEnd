package com.ll.quizzle.domain.avatar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ll.quizzle.domain.avatar.dto.request.AvatarCreateRequest;
import com.ll.quizzle.domain.avatar.service.AvatarService;
import com.ll.quizzle.global.response.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/avatars/admin")
@Tag(name = "AdminAvatarController", description = "관리자용 아바타 관리 API")
public class AdminAvatarController {

	private final AvatarService avatarService;

	@PostMapping
	@Operation(summary = "아바타 등록", description = "관리자가 새로운 아바타를 등록합니다.")
	public RsData<String> createAvatar(@Valid @RequestBody AvatarCreateRequest request) {
		avatarService.createAvatar(request);
		return RsData.success(HttpStatus.CREATED, "아바타가 성공적으로 등록되었습니다.");
	}
}
