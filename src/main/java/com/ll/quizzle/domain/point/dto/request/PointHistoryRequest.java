package com.ll.quizzle.domain.point.dto.request;

import com.ll.quizzle.domain.point.type.PointType;

public record PointHistoryRequest(
	PointType type, // "REWARD" or "USE", nullable
	int page,
	int size
) {
	public PointHistoryRequest {
		// 기본값 설정
		if (page < 0)
			page = 0;
		if (size <= 0)
			size = 10;
	}
}
