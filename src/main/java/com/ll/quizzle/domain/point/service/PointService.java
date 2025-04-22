package com.ll.quizzle.domain.point.service;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.point.dto.request.PointHistoryRequest;
import com.ll.quizzle.domain.point.dto.response.PointHistoryResponse;
import com.ll.quizzle.domain.point.entity.Point;
import com.ll.quizzle.domain.point.repository.PointRepository;
import com.ll.quizzle.domain.point.type.PointReason;
import com.ll.quizzle.domain.point.type.PointType;
import com.ll.quizzle.global.request.Rq;
import com.ll.quizzle.standard.page.dto.PageDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

	private final PointRepository pointRepository;
	private final MemberRepository memberRepository;
	private final Rq rq;

	public PageDto<PointHistoryResponse> getPointHistoriesWithPage(Long memberId,
		PointHistoryRequest requestDto) {

		memberRepository.findById(memberId)
			.orElseThrow(MEMBER_NOT_FOUND::throwServiceException);

		Member member = rq.assertIsOwner(memberId);

		Pageable pageable = PageRequest.of(requestDto.page(), requestDto.size());

		Page<Point> page = (requestDto.type() == PointType.ALL) ?
			pointRepository.findPageByMemberOrderByOccurredAtDesc(member, pageable) :
			pointRepository.findPageByMemberAndTypeOrderByOccurredAtDesc(member, requestDto.type(), pageable);

		return new PageDto<>(page.map(PointHistoryResponse::from));
	}

	// 포인트 사용
	public void usePoint(Member member, int amount, PointReason reason) {
		member.decreasePoint(amount);
		Point point = new Point(member, -amount, PointType.USE, reason);
		pointRepository.save(point);
	}

	// 포인트 획득
	public void gainPoint(Member member, int amount, PointReason reason) {
		member.increasePoint(amount);
		Point point = new Point(member, amount, PointType.REWARD, reason);
		pointRepository.save(point);
	}

	// 정책 기반 포인트 처리
	public void applyPointPolicy(Member member, PointReason reason) {
		int amount = reason.getDefaultAmount();

		if (amount == 0) {
			throw POINT_POLICY_NOT_FOUND.throwServiceException();
		}

		if (amount < 0) {
			usePoint(member, Math.abs(amount), reason);
		} else {
			gainPoint(member, amount, reason);
		}
	}

	public void applyPointPolicy(Member member, int customAmount, PointReason reason) {
		if (customAmount == 0) {
			throw POINT_POLICY_NOT_FOUND.throwServiceException();
		}

		if (customAmount < 0) {
			usePoint(member, Math.abs(customAmount), reason);
		} else {
			gainPoint(member, customAmount, reason);
		}
	}

}
