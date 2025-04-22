package com.ll.quizzle.global.socket.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 채팅 메시지 타입
 */
@Getter
@RequiredArgsConstructor
public enum MessageType {
    CHAT("일반 채팅"),
    JOIN("입장 메시지"),
    LEAVE("퇴장 메시지"),
    SYSTEM("시스템 메시지"),
    WHISPER("귓속말");
    
    private final String description;
} 