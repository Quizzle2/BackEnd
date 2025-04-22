package com.ll.quizzle.domain.avatar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ll.quizzle.domain.avatar.entity.Avatar;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {
    Optional<Avatar> findByFileName(String fileName);
    boolean existsByFileName(String fileName);
}
