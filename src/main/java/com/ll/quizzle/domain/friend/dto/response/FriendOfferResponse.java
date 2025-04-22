package com.ll.quizzle.domain.friend.dto.response;

import com.ll.quizzle.domain.friend.entity.FriendOffer;

import java.time.LocalDateTime;

public record FriendOfferResponse(
        long memberId,
        String nickname,
        String status,
        LocalDateTime responseAt
) {
    public static FriendOfferResponse from(FriendOffer friendOffer) {
        return new FriendOfferResponse(
                friendOffer.getFromMember().getId(),
                friendOffer.getFromMember().getNickname(),
                friendOffer.getStatus().name(),
                friendOffer.getModifyDate()
        );
    }
}
