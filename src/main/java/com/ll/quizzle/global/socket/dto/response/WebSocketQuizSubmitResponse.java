package com.ll.quizzle.global.socket.dto.response;

import com.ll.quizzle.global.socket.type.RoomMessageType;

public record WebSocketQuizSubmitResponse(
        RoomMessageType type,
        int questionNumber,
        boolean correct,
        String correctAnswer,
        String memberId,
        String nickname,
        boolean isSubmitted,
        long timestamp,
        String quizId
) {}
