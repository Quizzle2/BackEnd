package com.ll.quizzle.factory;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.point.entity.Point;
import com.ll.quizzle.domain.point.repository.PointRepository;
import com.ll.quizzle.domain.point.type.PointReason;
import com.ll.quizzle.domain.point.type.PointType;

public class PointTestFactory {

	public static void createRewardPoint(Member member, PointRepository repo, int amount) {
		member.increasePoint(amount);
		Point point = Point.builder()
			.member(member)
			.amount(amount)
			.type(PointType.REWARD)
			.reason(PointReason.LEVEL_UP)
			.build();
		repo.save(point);
	}

	public static void createUsePoint(Member member, PointRepository repo, int amount) {
		member.decreasePoint(amount);
		Point point = Point.builder()
			.member(member)
			.amount(-amount)
			.type(PointType.USE)
			.reason(PointReason.NICKNAME_CHANGE)
			.build();
		repo.save(point);
	}
}
