package com.ll.quizzle.domain.point.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.point.type.PointReason;
import com.ll.quizzle.domain.point.type.PointType;
import com.ll.quizzle.global.jpa.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Point extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	private int amount;

	@Enumerated(EnumType.STRING)
	private PointType type;

	@Enumerated(EnumType.STRING)
	private PointReason reason;

	@CreatedDate
	private LocalDateTime occurredAt;

	@Builder
	public Point(Member member, int amount, PointType type, PointReason reason) {
		this.member = member;
		this.amount = amount;
		this.type = type;
		this.reason = reason;
	}

	public String getDescription() {
		return reason.getDescription();
	}
}
