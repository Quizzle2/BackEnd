package com.ll.quizzle.domain.system.dto.response;

import com.ll.quizzle.domain.member.type.Role;

public record RoleChangeResponse(
	String targetEmail,
	Role previousRole,
	Role newRole,
	String reason
) {
	public static RoleChangeResponse from(
		String targetEmail,
		Role previousRole,
		Role newRole,
		String reason
	) {
		return new RoleChangeResponse(
			targetEmail,
			previousRole,
			newRole,
			reason
		);
	}
}
