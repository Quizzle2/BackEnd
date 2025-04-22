package com.ll.quizzle.domain.member.entity;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;

import java.util.ArrayList;
import java.util.List;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.entity.OwnedAvatar;
import com.ll.quizzle.domain.member.type.Role;
import com.ll.quizzle.global.jpa.entity.BaseTime;
import com.ll.quizzle.global.security.oauth2.entity.OAuth;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTime {

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private int level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private int exp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_id")
    private Avatar avatar;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OwnedAvatar> ownedAvatars = new ArrayList<>();

    @Column(nullable = false)
    private int pointBalance;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private OAuth oauth;

    @Builder
    private Member(String nickname, String email, int level, Role role, int exp, int pointBalance, Avatar avatar) {
        this.nickname = nickname;
        this.email = email;
        this.level = level;
        this.role = role;
        this.exp = exp;
        this.pointBalance = pointBalance;
        this.avatar = avatar;
    }

    public static Member create(String nickname, String email, Avatar avatar) {
        return Member.builder()
            .nickname(nickname)
            .email(email)
            .level(1)
            .role(Role.MEMBER)
            .exp(0)
            .pointBalance(0)
            .avatar(avatar)
            .build();
    }

    public String getUserRole() {
        return this.role.name();
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public boolean isMember() {
        return this.role == Role.MEMBER;
    }

    public void increasePoint(int amount) {
        if (amount <= 0) POINT_INCREASE_AMOUNT_INVALID.throwServiceException();
        this.pointBalance += amount;
    }

    public void decreasePoint(int amount) {
        if (amount <= 0) POINT_DECREASE_AMOUNT_INVALID.throwServiceException();
        if (this.pointBalance < amount) POINT_NOT_ENOUGH.throwServiceException();
        this.pointBalance -= amount;
    }

    public void updateExp(int newExp) {
        this.exp = newExp;
        int calculatedLevel = newExp / 100;
        if (calculatedLevel > this.level) {
            this.level = calculatedLevel;
        }
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeAvatar(Avatar avatar) {
        this.avatar = avatar;
    }

    public void addOwnedAvatar(OwnedAvatar ownedAvatar) {
        this.ownedAvatars.add(ownedAvatar);
    }

    public boolean hasAvatar(Avatar avatar) {
        return ownedAvatars.stream()
            .anyMatch(owned -> owned.getAvatar().getId().equals(avatar.getId()));
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
    }
}
