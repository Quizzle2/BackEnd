package com.ll.quizzle.domain.avatar.dto.response;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.entity.OwnedAvatar;
import com.ll.quizzle.domain.avatar.type.AvatarStatus;

public record AvatarPurchaseResponse(
    Long id,
    String fileName,
    int price,
    AvatarStatus status,
    String url
) {
    // 소유한 아바타 응답 (OwnedAvatar 기반)
    public static AvatarPurchaseResponse from(OwnedAvatar ownedAvatar) {
        Avatar avatar = ownedAvatar.getAvatar();
        return new AvatarPurchaseResponse(
            avatar.getId(),
            avatar.getFileName(),
            avatar.getPrice(),
            ownedAvatar.getStatus(),
            avatar.getUrl()
        );
    }

    // 구매 가능한 아바타 응답 (Avatar 기반)
    public static AvatarPurchaseResponse from(Avatar avatar) {
        return new AvatarPurchaseResponse(
            avatar.getId(),
            avatar.getFileName(),
            avatar.getPrice(),
            null, // 상태 없음
            avatar.getUrl()
        );
    }
}
