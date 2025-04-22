package com.ll.quizzle.domain.member.dto.response;

import com.ll.quizzle.domain.member.entity.Member;

public record MemberProfileEditResponse(
	Long id,
	String profile
) {
	public static MemberProfileEditResponse from(Member member) {
		return new MemberProfileEditResponse(member.getId(), member.getNickname());
	}
}

