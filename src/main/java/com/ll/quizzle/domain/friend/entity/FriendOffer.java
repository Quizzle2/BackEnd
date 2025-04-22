package com.ll.quizzle.domain.friend.entity;

import com.ll.quizzle.domain.friend.type.FriendRequestStatus;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.global.jpa.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friend_offer", uniqueConstraints = @UniqueConstraint(columnNames = {"from_member_id", "to_member_id"}))
public class FriendOffer extends BaseTime {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_member_id", nullable = false)
    private Member fromMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_member_id", nullable = false)
    private Member toMember;

    @Enumerated(EnumType.STRING)
    private FriendRequestStatus status;

    @Version
    private Integer version;

    @Builder
    private FriendOffer(Member fromMember, Member toMember, FriendRequestStatus status) {
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.status = status;
    }

    public static FriendOffer create(Member fromMember, Member toMember) {
        return FriendOffer.builder()
                .fromMember(fromMember)
                .toMember(toMember)
                .status(FriendRequestStatus.PENDING)
                .build();
    }

    public void changeStatus(FriendRequestStatus status) {
        this.status = status;
    }
}
