package com.ll.quizzle.domain.friend.dto.response;

import com.ll.quizzle.domain.friend.entity.FriendOffer;

import java.time.LocalDateTime;

public record FriendResponse(
        long fromMemberId,
        long toMemberId,
        String status,
        LocalDateTime requestedAt
) {
    public static FriendResponse from(FriendOffer friendOffer) {
        return new FriendResponse(
                friendOffer.getFromMember().getId(),
                friendOffer.getToMember().getId(),
                friendOffer.getStatus().name(),
                friendOffer.getCreateDate()
        );
    }
}
