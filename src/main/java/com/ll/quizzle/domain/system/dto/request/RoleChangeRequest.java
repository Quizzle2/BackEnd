package com.ll.quizzle.domain.system.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ll.quizzle.domain.member.type.Role;

public record RoleChangeRequest(
	String targetEmail,
	Role newRole,
	String reason,
	String secondaryPassword,
	@JsonIgnore String changeBy
) {
	public static RoleChangeRequest of(
		String targetEmail,
		Role newRole,
		String reason,
		String secondaryPassword
	) {
		return new RoleChangeRequest(targetEmail, newRole, reason, secondaryPassword, null);
	}
}
