package com.ll.quizzle.domain.friend.dto.response;

import com.ll.quizzle.domain.friend.entity.FriendOffer;

import java.time.LocalDateTime;

public record FriendOfferListResponse(
        long memberId,
        String nickname,
        LocalDateTime requestedAt
) {
    public static FriendOfferListResponse from(FriendOffer friendOffer) {
        return new FriendOfferListResponse(
                friendOffer.getFromMember().getId(),
                friendOffer.getFromMember().getNickname(),
                friendOffer.getCreateDate()
        );
    }
}
