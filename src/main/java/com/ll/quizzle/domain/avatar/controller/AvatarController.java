package com.ll.quizzle.domain.avatar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ll.quizzle.domain.avatar.dto.response.AvatarPurchaseResponse;
import com.ll.quizzle.domain.avatar.service.AvatarService;
import com.ll.quizzle.global.response.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/{memberId}/avatars")
@Tag(name = "AvatarController", description = "아바타 구매 및 조회 API")
public class AvatarController {

    private final AvatarService avatarService;

    @PostMapping("/{avatarId}")
    @Operation(summary = "아바타 구매", description = "사용자가 아바타를 구매합니다.")
    public RsData<String> purchaseAvatar(
        @Parameter(description = "회원 ID") @PathVariable Long memberId,
        @Parameter(description = "아바타 ID") @PathVariable Long avatarId) {
        avatarService.purchaseAvatar(memberId, avatarId);
        return RsData.success(HttpStatus.OK, "아바타 구매가 완료되었습니다.");
    }

    @GetMapping("/owned")
    @Operation(summary = "소유한 아바타 목록", description = "사용자가 소유한 아바타 목록을 조회합니다.")
    public RsData<List<AvatarPurchaseResponse>> getOwnedAvatars(
        @Parameter(description = "회원 ID") @PathVariable Long memberId) {
        List<AvatarPurchaseResponse> response = avatarService.getOwnedAvatars(memberId);
        return RsData.success(HttpStatus.OK, response);
    }

    @GetMapping("/available")
    @Operation(summary = "구매 가능한 아바타 목록", description = "사용자가 구매 가능한(아직 구매하지 않은) 아바타 목록을 조회합니다.")
    public RsData<List<AvatarPurchaseResponse>> getAvailableAvatars(
        @Parameter(description = "회원 ID") @PathVariable Long memberId) {
        List<AvatarPurchaseResponse> response = avatarService.getAvailableAvatars(memberId);
        return RsData.success(HttpStatus.OK, response);
    }
}
