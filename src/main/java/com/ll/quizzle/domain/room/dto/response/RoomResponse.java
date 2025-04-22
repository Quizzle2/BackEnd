package com.ll.quizzle.domain.room.dto.response;

import com.ll.quizzle.domain.room.entity.Room;
import com.ll.quizzle.domain.room.type.MainCategory;
import com.ll.quizzle.domain.room.type.RoomStatus;
import com.ll.quizzle.domain.room.type.Difficulty;
import com.ll.quizzle.domain.room.type.SubCategory;

import java.util.Set;

public record RoomResponse(
    Long id,
    String title,
    Long ownerId,
    String ownerNickname,
    int capacity,
    int currentPlayers,
    RoomStatus status,
    Difficulty difficulty,
    MainCategory mainCategory,
    SubCategory subCategory,
    String password,
    int problemCount,
    boolean isPrivate,
    Set<Long> players,
    Set<Long> readyPlayers
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
            room.getId(),
            room.getTitle(),
            room.getOwner().getId(),
            room.getOwner().getNickname(),
            room.getCapacity(),
            room.getPlayers().size(),
            room.getStatus(),
            room.getDifficulty(),
            room.getMainCategory(),
            room.getSubCategory(),
            room.getPasswordHash(),
            room.getProblemCount(),
            room.isPrivate(),
            room.getPlayers(),
            room.getReadyPlayers()
        );
    }
} 