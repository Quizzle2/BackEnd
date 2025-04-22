package com.ll.quizzle.domain.avatar.service;

import static com.ll.quizzle.global.exceptions.ErrorCode.*;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ll.quizzle.domain.avatar.dto.request.AvatarCreateRequest;
import com.ll.quizzle.domain.avatar.dto.response.AvatarPurchaseResponse;
import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.entity.OwnedAvatar;
import com.ll.quizzle.domain.avatar.repository.AvatarRepository;
import com.ll.quizzle.domain.avatar.repository.OwnedAvatarRepository;
import com.ll.quizzle.domain.avatar.type.AvatarStatus;
import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.point.service.PointService;
import com.ll.quizzle.domain.point.type.PointReason;
import com.ll.quizzle.global.request.Rq;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvatarService {

    private final AvatarRepository avatarRepository;
    private final OwnedAvatarRepository ownedAvatarRepository;
    private final PointService pointService;
    private final Rq rq;

    // 아바타 등록 매서드
    public void createAvatar(AvatarCreateRequest request) {

        Member admin = rq.getActor();

        if (!admin.isAdmin()) {
            throw FORBIDDEN_ACCESS.throwServiceException();
        }

        Avatar avatar = Avatar.builder()
            .fileName(request.fileName())
            .url(request.url())
            .price(request.price())
            .build();

        avatarRepository.save(avatar);
    }

    @Transactional
    public void purchaseAvatar(Long memberId, Long avatarId) {
        Member member = rq.assertIsOwner(memberId);

        Avatar avatar = avatarRepository.findById(avatarId)
            .orElseThrow(AVATAR_NOT_FOUND::throwServiceException);

        if (member.hasAvatar(avatar)) {
            throw AVATAR_ALREADY_OWNED.throwServiceException();
        }

        int price = avatar.getPrice();
        if (price > 0) {
            pointService.applyPointPolicy(member, -price, PointReason.AVATAR_PURCHASE);
        }

        OwnedAvatar ownedAvatar = OwnedAvatar.create(member, avatar);
        member.addOwnedAvatar(ownedAvatar);
        ownedAvatarRepository.save(ownedAvatar);
    }

    // 구매하지 않은 아바타 목록 조회
    public List<AvatarPurchaseResponse> getAvailableAvatars(Long memberId) {
        Member member = rq.assertIsOwner(memberId);

        List<Avatar> allAvatars = avatarRepository.findAll();
        List<Long> ownedAvatarIds = member.getOwnedAvatars().stream()
            .map(owned -> owned.getAvatar().getId())
            .toList();

        return allAvatars.stream()
            .filter(avatar -> !ownedAvatarIds.contains(avatar.getId()))
            .map(AvatarPurchaseResponse::from)
            .toList();
    }

    // 소유한 아바타 목록 조회
    public List<AvatarPurchaseResponse> getOwnedAvatars(Long memberId) {
        Member member = rq.assertIsOwner(memberId);

        List<OwnedAvatar> ownedAvatars = ownedAvatarRepository.findAllByMember(member);

        return ownedAvatars.stream()
            .map(AvatarPurchaseResponse::from)
            .toList();
    }
}
