package com.ll.quizzle.domain.member.controller;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ll.quizzle.domain.member.dto.request.MemberProfileEditRequest;
import com.ll.quizzle.domain.member.dto.response.MemberProfileEditResponse;
import com.ll.quizzle.domain.member.dto.response.MemberRankingResponse;
import com.ll.quizzle.domain.member.dto.response.UserProfileResponse;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.global.request.Rq;
import com.ll.quizzle.global.response.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@Tag(name = "MemberController", description = "회원 관련 API")
public class MemberController {
    private final MemberService memberService;
    private final Rq rq;

    @GetMapping("/{memberId}")
    @Operation(summary = "회원 프로필 조회", description = "회원의 프로필 정보를 조회합니다.")
    public RsData<UserProfileResponse> getUserProfile(@PathVariable Long memberId) {
        Member member = memberService.findById(memberId).orElseThrow(MEMBER_NOT_FOUND::throwServiceException);
        return RsData.success(HttpStatus.OK, UserProfileResponse.of(member));
    }

    @GetMapping("/search")
    @Operation(summary = "닉네임으로 사용자 검색", description = "닉네임으로 회원을 검색합니다.")
    public RsData<List<UserProfileResponse>> searchByNickname(@RequestParam String nickname) {
        List<UserProfileResponse> responses = memberService.searchUserProfilesByNickname(nickname);
        return RsData.success(HttpStatus.OK, responses);
    }

    @PatchMapping("/{memberId}/nickname")
    @Operation(summary = "닉네임 수정", description = "닉네임 정보를 수정합니다.")
    public RsData<MemberProfileEditResponse> editNickname(
        @PathVariable Long memberId,
        @RequestBody MemberProfileEditRequest request
    ) {
        MemberProfileEditResponse response = memberService.editNickname(memberId, request.nickname());
        return RsData.success(HttpStatus.OK, response);
    }

    @PatchMapping("/{memberId}/avatars/{avatarId}")
    @Operation(summary = "아바타 수정", description = "아바타 정보를 수정합니다.")
    public RsData<String> editAvatar(
        @PathVariable Long memberId,
        @PathVariable Long avatarId
    ) {
        memberService.editAvatar(memberId, avatarId);
        return RsData.success(HttpStatus.OK, "아바타 수정이 완료되었습니다.");
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "로그아웃", description = "로그아웃 처리합니다.")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        memberService.logout(request, response);
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "내 정보를 조회합니다.")
    public RsData<UserProfileResponse> getMyProfile() {
        Member actor = rq.getActor();
        Member member = memberService.findById(actor.getId()).orElseThrow(MEMBER_NOT_FOUND::throwServiceException);
        return RsData.success(HttpStatus.OK, UserProfileResponse.of(member));
    }
    
    @GetMapping("/rankings")
    @Operation(summary = "경험치 랭킹 조회", description = "모든 회원의 경험치 랭킹을 조회합니다. 경험치 내림차순으로 정렬됩니다.")
    public RsData<List<MemberRankingResponse>> getRankings() {
        List<MemberRankingResponse> rankingResponses = memberService.getMemberRankings();
        return RsData.success(HttpStatus.OK, rankingResponses);
    }
}
