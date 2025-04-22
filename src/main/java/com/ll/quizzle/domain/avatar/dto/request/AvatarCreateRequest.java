package com.ll.quizzle.domain.avatar.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AvatarCreateRequest(
	@NotBlank(message = "파일명은 필수입니다.")
	String fileName,

	@NotBlank(message = "URL은 필수입니다.")
	String url,

	@Min(value = 0, message = "가격은 0 이상이어야 합니다.")
	int price
) {}

