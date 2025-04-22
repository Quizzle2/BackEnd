package com.ll.quizzle.domain.room.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ll.quizzle.domain.member.entity.Member;
import com.ll.quizzle.domain.room.entity.Room;
import com.ll.quizzle.domain.room.entity.RoomBlacklist;

public interface RoomBlacklistRepository extends JpaRepository<RoomBlacklist, Long> {
    boolean existsByRoomAndMember(Room room, Member member);
    void deleteByRoomAndMember(Room room, Member member);
    List<RoomBlacklist> findByRoom(Room room);
    List<RoomBlacklist> findByMember(Member member);
} 