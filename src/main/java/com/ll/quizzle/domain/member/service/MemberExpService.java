package com.ll.quizzle.domain.member.service;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.member.repository.MemberRepository;
import com.ll.quizzle.domain.point.service.PointService;
import com.ll.quizzle.domain.point.type.PointReason;
import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.exceptions.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberExpService {

    private final MemberRepository memberRepository;
    private final PointService pointService; // PointService 주입

    public MemberExpService(MemberRepository memberRepository, PointService pointService) {
        this.memberRepository = memberRepository;
        this.pointService = pointService;
    }

    @Transactional
    public void updateMemberExp(Long memberId, int score) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(
                        ErrorCode.MEMBER_NOT_FOUND.getHttpStatus(),
                        ErrorCode.MEMBER_NOT_FOUND.getMessage()
                ));

        // 기존 레벨을 저장
        int oldLevel = member.getLevel();

        int newExp = member.getExp() + score;
        member.updateExp(newExp);

        if (member.getLevel() > oldLevel) {
            pointService.applyPointPolicy(member, PointReason.LEVEL_UP);
        }

        memberRepository.save(member);
    }
}
