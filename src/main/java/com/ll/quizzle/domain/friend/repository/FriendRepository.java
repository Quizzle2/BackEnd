package com.ll.quizzle.domain.friend.repository;

import com.ll.quizzle.domain.friend.entity.Friend;
import com.ll.quizzle.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    boolean existsByMemberAndFriend(Member member, Member friend);
    
    List<Friend> findAllByMemberOrderByFriendNicknameAsc(Member member);

    Optional<Friend> findByMemberAndFriend(Member member, Member friend);
}
