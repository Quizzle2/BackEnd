package com.ll.quizzle.domain.room.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubCategory {
    PHYSICS("물리학", MainCategory.SCIENCE),
    CHEMISTRY("화학", MainCategory.SCIENCE),
    BIOLOGY("생물학", MainCategory.SCIENCE),

    WORLD_HISTORY("세계사", MainCategory.HISTORY),
    KOREAN_HISTORY("한국사", MainCategory.HISTORY),

    KOREAN("한국어", MainCategory.LANGUAGE),
    ENGLISH("영어", MainCategory.LANGUAGE),

    CURRENT_AFFAIRS("시사", MainCategory.GENERAL_KNOWLEDGE),
    CULTURE("문화", MainCategory.GENERAL_KNOWLEDGE),
    SPORTS("스포츠", MainCategory.GENERAL_KNOWLEDGE);

    private final String description;
    private final MainCategory mainCategory;
}
