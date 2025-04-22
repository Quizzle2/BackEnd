package com.ll.quizzle.domain.member.dto.response;

import com.ll.quizzle.domain.member.entity.Member;

public record UserProfileResponse(
    Long id,
    String nickname,
    String avatarUrl,
    int level,
    int exp,
    int point
) {
    public static UserProfileResponse of(Member member) {
        return new UserProfileResponse(
            member.getId(),
            member.getNickname(),
            member.getAvatar().getUrl(),
            member.getLevel(),
            member.getExp(),
            member.getPointBalance()
        );
    }
}
