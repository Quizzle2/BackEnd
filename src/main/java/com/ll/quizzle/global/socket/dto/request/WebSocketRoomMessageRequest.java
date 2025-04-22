package com.ll.quizzle.global.socket.dto.request;

import com.ll.quizzle.global.exceptions.ErrorCode;
import com.ll.quizzle.global.socket.type.RoomMessageType;

/**
 * 클라이언트로부터 받는 게임 상태, 준비 상태 등의 메시지 요청을 위한 DTO
 */
public record WebSocketRoomMessageRequest(
    RoomMessageType type,
    String content,
    String data,
    String senderId,
    String senderName,
    long timestamp,
    String roomId
) {
    public WebSocketRoomMessageRequest {
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

    public static WebSocketRoomMessageRequest of(
            RoomMessageType type,
            String content,
            String data,
            String senderId,
            String senderName,
            long timestamp,
            String roomId
    ) {
        return new WebSocketRoomMessageRequest(type, content, data, senderId, senderName, timestamp, roomId);
    }
} 