package com.ll.quizzle.domain.friend.repository;

import com.ll.quizzle.domain.friend.entity.FriendOffer;
import com.ll.quizzle.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendOfferRepository extends JpaRepository<FriendOffer, Long> {
    boolean existsByFromMemberAndToMember(Member fromMember, Member toMember);

    Optional<FriendOffer> findByFromMemberAndToMember(Member fromMember, Member toMember);

    List<FriendOffer> findAllByToMemberOrderByCreateDateAsc(Member member);
}
