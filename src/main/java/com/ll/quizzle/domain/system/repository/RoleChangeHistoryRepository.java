package com.ll.quizzle.domain.system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.system.entity.RoleChangeHistory;

public interface RoleChangeHistoryRepository extends JpaRepository<RoleChangeHistory, Long> {
	Optional<RoleChangeHistory> findTopByMemberOrderByCreateDateDesc(Member member);
}
