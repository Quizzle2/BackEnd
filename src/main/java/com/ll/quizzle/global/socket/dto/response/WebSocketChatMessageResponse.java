package com.ll.quizzle.global.socket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ll.quizzle.global.socket.type.MessageType;

/**
 * 클라이언트에게 전송되는 채팅 메시지 응답을 위한 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSocketChatMessageResponse(
    MessageType type,
    String content,
    Long senderId,
    String senderName,
    long timestamp,
    String roomId
) {

    public static WebSocketChatMessageResponse of(
            MessageType type,
            String content,
            Long senderId,
            String senderName,
            long timestamp,
            String roomId
    ) {
        return new WebSocketChatMessageResponse(type, content, senderId, senderName, timestamp, roomId);
    }
} 