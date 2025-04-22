package com.ll.quizzle.domain.system.entity;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.type.Role;
import com.ll.quizzle.global.jpa.entity.BaseTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleChangeHistory extends BaseTime {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 권한이 바뀐 대상

    @Column(nullable = false)
    private String changedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role previousRole;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role newRole;

    @Column(length = 500)
    private String reason;

    @Builder
    public RoleChangeHistory(
        Member member,
        String changedBy,
        Role previousRole,
        Role newRole,
        String reason
    ) {
        this.member = member;
        this.changedBy = changedBy;
        this.previousRole = previousRole;
        this.newRole = newRole;
        this.reason = reason;
    }
}
