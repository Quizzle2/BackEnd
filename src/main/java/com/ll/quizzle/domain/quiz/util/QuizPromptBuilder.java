package com.ll.quizzle.domain.quiz.util;

import com.ll.quizzle.domain.quiz.dto.request.QuizGenerationRequest;
import com.ll.quizzle.domain.room.type.AnswerType;

public class QuizPromptBuilder {
    public static String buildPrompt(QuizGenerationRequest request) {
        String optionText = request.answerType() == AnswerType.MULTIPLE_CHOICE
                ? "a) 보기1\nb) 보기2\nc) 보기3\nd) 보기4"
                : "O 또는 X";

        return String.format(
                "너는 한국어 전문 퀴즈 생성기야. **절대로 영어 사용 금지**. 인삿말이나 추가 설명 없이 오직 문제와 정답만 출력해.\n" +
                        "대분류: %s\n소분류: %s\n문제 유형: %s\n문제 수: %d\n난이도: %s\n\n각 문제 형식:\n문제 번호. 문제 내용\n%s\n정답: <정답>",
                request.mainCategory(), request.subCategory(),
                request.answerType(), request.problemCount(), request.difficulty(),
                optionText
        );
    }
}
