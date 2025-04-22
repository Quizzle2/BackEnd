package com.ll.quizzle.domain.point.dto.response;

import java.time.LocalDateTime;

import com.ll.quizzle.domain.point.entity.Point;

public record PointHistoryResponse(
	String type,
	int amount,
	String description,
	LocalDateTime createdAt
) {
	public static PointHistoryResponse from(Point point) {
		return new PointHistoryResponse(
			point.getType().name(),
			point.getAmount(),
			point.getReason().getDescription(),
			point.getOccurredAt()
		);
	}
}
