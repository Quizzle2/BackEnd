package com.ll.quizzle.global.socket.dto.request;

import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.socket.type.MessageType;

/**
 * 클라이언트로부터 받는 채팅 메시지 요청을 위한 DTO
 */
public record WebSocketChatMessageRequest(
    MessageType type,
    String content,
    String senderId,
    String senderName,
    long timestamp,
    String roomId
) {
    public WebSocketChatMessageRequest {
        if (type == null) {
            ErrorCode.WEBSOCKET_MESSAGE_TYPE_REQUIRED.throwServiceException();
        }
        if (senderId == null || senderId.trim().isEmpty()) {
            ErrorCode.WEBSOCKET_SENDER_ID_REQUIRED.throwServiceException();
        }
        if (senderName == null || senderName.trim().isEmpty()) {
            ErrorCode.WEBSOCKET_SENDER_NAME_REQUIRED.throwServiceException();
        }
        if (roomId == null || roomId.trim().isEmpty()) {
            ErrorCode.WEBSOCKET_ROOM_ID_REQUIRED.throwServiceException();
        }
        if (timestamp <= 0) {
            ErrorCode.WEBSOCKET_TIMESTAMP_INVALID.throwServiceException();
        }
    }

    public static WebSocketChatMessageRequest of(
            MessageType type,
            String content,
            String senderId,
            String senderName,
            long timestamp,
            String roomId
    ) {
        return new WebSocketChatMessageRequest(type, content, senderId, senderName, timestamp, roomId);
    }
} 