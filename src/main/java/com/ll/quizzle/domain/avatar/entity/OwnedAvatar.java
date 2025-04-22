package com.ll.quizzle.domain.avatar.entity;

import com.ll.quizzle.domain.avatar.type.AvatarStatus;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OwnedAvatar extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_id")
    private Avatar avatar;

    @Enumerated(EnumType.STRING)
    private AvatarStatus status;

    @Builder
    public OwnedAvatar(Member member, Avatar avatar, AvatarStatus status) {
        this.member = member;
        this.avatar = avatar;
        this.status = status;
    }

    public static OwnedAvatar create(Member member, Avatar avatar) {
        return OwnedAvatar.builder()
            .member(member)
            .avatar(avatar)
            .status(AvatarStatus.OWNED)
            .build();
    }

    public boolean isOwned() {
        return this.status == AvatarStatus.OWNED;
    }
}
