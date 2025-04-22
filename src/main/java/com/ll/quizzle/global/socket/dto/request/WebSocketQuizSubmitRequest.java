package com.ll.quizzle.global.socket.dto.request;


public record WebSocketQuizSubmitRequest(
        int questionNumber,
        String submittedAnswer
) {}