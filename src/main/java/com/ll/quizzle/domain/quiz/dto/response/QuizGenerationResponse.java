package com.ll.quizzle.domain.quiz.dto.response;

import java.util.Map;

/**
 * GPT API를 통해 생성된 퀴즈 텍스트와 문제 번호별 정답 매핑을 담는 DTO
// */

public record QuizGenerationResponse(
        Map<Integer, String> quizText,
        Map<Integer, String> answerMap
) { }