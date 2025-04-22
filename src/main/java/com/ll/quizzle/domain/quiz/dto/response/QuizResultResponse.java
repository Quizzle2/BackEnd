package com.ll.quizzle.domain.quiz.dto.response;

public record QuizResultResponse(
        String memberId,
        int correctCount,
        int totalQuestions,
        int score,
        int rank,
        int exp
) {}
