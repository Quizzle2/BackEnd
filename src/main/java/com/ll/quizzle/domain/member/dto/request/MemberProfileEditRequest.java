package com.ll.quizzle.domain.member.dto.request;

import com.ll.quizzle.global.exceptions.ErrorCode;

public record MemberProfileEditRequest(String nickname) {

	public MemberProfileEditRequest {
		if (nickname == null || nickname.trim().isEmpty()) {
			ErrorCode.NICKNAME_INVALID.throwServiceException();
		}
		if (nickname.length() < 2 || nickname.length() > 20) {
			ErrorCode.NICKNAME_LENGTH_INVALID.throwServiceException();
		}
		if (!nickname.matches("^[a-zA-Z0-9가-힣]+$")) {
			ErrorCode.NICKNAME_FORMAT_INVALID.throwServiceException();
		}
		if (nickname.toUpperCase().startsWith("GUEST")) {
			ErrorCode.NICKNAME_GUEST_PREFIX_FORBIDDEN.throwServiceException();
		}
	}
}
