package com.ll.quizzle.domain.avatar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ll.quizzle.domain.avatar.entity.Avatar;
import com.ll.quizzle.domain.avatar.entity.OwnedAvatar;
import com.ll.quizzle.domain.member.entity.Member;

public interface OwnedAvatarRepository extends JpaRepository<OwnedAvatar, Long> {

    // 특정 사용자가 해당 아바타를 소유하고 있는지 여부 확인
    boolean existsByMemberAndAvatar(Member member, Avatar avatar);

    // 특정 유저가 소유한 특정 아바타 조회 (선택적으로 활용 가능)
    Optional<OwnedAvatar> findByMemberAndAvatar(Member member, Avatar avatar);

    List<OwnedAvatar> findAllByMember(Member member);

}
