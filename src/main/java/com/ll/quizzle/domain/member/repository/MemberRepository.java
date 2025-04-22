package com.ll.quizzle.domain.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ll.quizzle.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String memberEmail);
    
    @Query("SELECT m FROM Member m WHERE LOWER(m.nickname) LIKE CONCAT('%', LOWER(:keyword), '%')")
    List<Member> findByNicknameContainingIgnoreCase(@Param("keyword") String keyword);
    
    boolean existsByNickname(String nickname);
    
    List<Member> findAllByOrderByExpDesc();
}
