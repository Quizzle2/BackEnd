package com.ll.quizzle.domain.friend.dto.response;

import com.ll.quizzle.domain.friend.entity.Friend;

import java.time.LocalDateTime;

public record FriendListResponse(
        long memberId,
        String nickname,
        int level,
        boolean isOnline,
        LocalDateTime acceptedAt
) {
    public static FriendListResponse from(Friend friend) {
        return new FriendListResponse(
                friend.getFriend().getId(),
                friend.getFriend().getNickname(),
                friend.getFriend().getLevel(),
                friend.isOnline(),
                friend.getCreateDate()
        );
    }
}
