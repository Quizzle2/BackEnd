package com.ll.quizzle.domain.room.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MainCategory {
    SCIENCE("과학"),
    HISTORY("역사"),
    LANGUAGE("언어"),
    GENERAL_KNOWLEDGE("일반 상식");
    
    private final String description;
} 