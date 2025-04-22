package com.ll.quizzle.domain.member.dto.response;

import com.ll.quizzle.domain.member.entity.Member;

public record MemberRankingResponse(
    Long id,
    String nickname,
    int level,
    int exp
) {
    public static MemberRankingResponse of(Member member) {
        return new MemberRankingResponse(
            member.getId(),
            member.getNickname(),
            member.getLevel(),
            member.getExp()
        );
    }
} 