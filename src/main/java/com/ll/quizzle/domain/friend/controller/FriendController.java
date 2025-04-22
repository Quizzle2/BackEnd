package com.ll.quizzle.domain.friend.controller;

import com.ll.quizzle.domain.friend.dto.response.FriendListResponse;
import com.ll.quizzle.domain.friend.dto.response.FriendOfferResponse;
import com.ll.quizzle.domain.friend.dto.response.FriendOfferListResponse;
import com.ll.quizzle.domain.friend.dto.response.FriendResponse;
import com.ll.quizzle.domain.friend.service.FriendService;
import com.ll.quizzle.domain.friend.type.FriendRequestStatus;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.global.request.Rq;
import com.ll.quizzle.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friends")
@Tag(name = "친구 관리", description = "친구 요청, 수락, 거절 등의 API")
public class FriendController {
    private final FriendService friendService;
    private final Rq rq;

    @PostMapping("/{memberId}/request")
    @Operation(summary = "친구 요청", description = "특정 사용자에게 친구 요청을 보냅니다.")
    public RsData<FriendResponse> sendFriendOffer(@PathVariable long memberId) {
        Member actor = rq.getActor();
        FriendResponse friendResponse = friendService.sendFriendOffer(actor.getId(), memberId);
        return RsData.success(HttpStatus.OK, friendResponse);
    }

    @PostMapping("/{memberId}/accept")
    @Operation(summary = "친구 요청 수락", description = "특정 사용자로부터 온 친구 요청을 수락합니다.")
    public RsData<FriendOfferResponse> acceptFriendOffer(@PathVariable long memberId) {
        Member actor = rq.getActor();
        FriendOfferResponse friendAcceptResponse = friendService.handleFriendOffer(actor.getId(), memberId, FriendRequestStatus.ACCEPTED);
        return RsData.success(HttpStatus.OK, friendAcceptResponse);
    }

    @PostMapping("/{memberId}/reject")
    @Operation(summary = "친구 요청 거절", description = "특정 사용자로부터 온 친구 요청을 거절합니다.")
    public RsData<FriendOfferResponse> rejectFriendOffer(@PathVariable long memberId) {
        Member actor = rq.getActor();
        FriendOfferResponse friendRejectResponse = friendService.handleFriendOffer(actor.getId(), memberId, FriendRequestStatus.REJECTED);
        return RsData.success(HttpStatus.OK, friendRejectResponse);
    }

    @GetMapping("/requests")
    @Operation(summary = "친구 요청 목록", description = "내가 받은 친구 요청 목록을 조회합니다.")
    public RsData<List<FriendOfferListResponse>> getFriendRequestList() {
        Member actor = rq.getActor();
        List<FriendOfferListResponse> friendOfferListResponse = friendService.getFriendOfferList(actor.getId());
        return RsData.success(HttpStatus.OK, friendOfferListResponse);
    }

    @GetMapping
    @Operation(summary = "친구 목록", description = "내 친구 목록을 조회합니다.")
    public RsData<List<FriendListResponse>> getFriendList() {
        Member actor = rq.getActor();
        List<FriendListResponse> friendListResponse = friendService.getFriendList(actor.getId());
        return RsData.success(HttpStatus.OK, friendListResponse);
    }

    @DeleteMapping("/{memberId}")
    @Operation(summary = "친구 삭제", description = "특정 친구를 삭제합니다.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFriend(@PathVariable long memberId) {
        Member actor = rq.getActor();
        friendService.deleteFriend(actor.getId(), memberId);
    }
}
