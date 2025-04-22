package com.ll.quizzle.domain.quiz.dto.request;

import com.ll.quizzle.domain.room.type.AnswerType;
import com.ll.quizzle.domain.room.type.Difficulty;
import com.ll.quizzle.domain.room.type.MainCategory;
import com.ll.quizzle.domain.room.type.SubCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;


public record QuizGenerationRequest(
        MainCategory mainCategory,
        SubCategory subCategory,
        AnswerType answerType,

        @Min(1)
        @Max(20)
        int problemCount,
        Difficulty difficulty,

        String roomId
) {
}
