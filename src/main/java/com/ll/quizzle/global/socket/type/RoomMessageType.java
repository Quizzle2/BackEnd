package com.ll.quizzle.global.socket.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 방 내부 메시지 타입
 */
@Getter
@RequiredArgsConstructor
public enum RoomMessageType {
    JOIN("방 입장"),
    LEAVE("방 퇴장"),
    READY("준비 완료"),
    UNREADY("준비 취소"),
    GAME_START("게임 시작"),
    GAME_END("게임 종료"),
    ANSWER_SUBMIT("정답 제출"),
    TIMER("타이머 업데이트"),
    ROUND_START("라운드 시작"),
    ROUND_END("라운드 종료"),
    SYSTEM("시스템 메시지"),
    ROOM_UPDATED("방 정보 업데이트");
    
    private final String description;
} 