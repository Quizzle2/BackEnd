package com.ll.quizzle.domain.room.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ll.quizzle.domain.room.entity.Room;
import com.ll.quizzle.domain.room.type.RoomStatus;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByStatusNot(RoomStatus status);
    
    Optional<Room> findRoomById(Long id);
    
    @Query("SELECT r FROM Room r JOIN r.players p WHERE p = :playerId")
    List<Room> findRoomsByPlayerId(@Param("playerId") Long playerId);
}
