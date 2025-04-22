package com.ll.quizzle.domain.room.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoomStatus {
    WAITING("대기 중"),
    IN_GAME("게임 중"),
    FINISHED("게임 종료");

    private final String description;
}
