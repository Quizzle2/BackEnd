package com.ll.quizzle.domain.quiz.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.quizzle.domain.quiz.dto.request.QuizGenerationRequest;
import com.ll.quizzle.domain.quiz.dto.response.QuizGenerationResponse;
import com.ll.quizzle.global.exceptions.ErrorCode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizResponseParser {

    private static final Pattern QUESTION_PATTERN =
            Pattern.compile("^(?:Question\\s+|문제\\s*)?(\\d+)\\.");
    private static final Pattern ANSWER_PATTERN =
            Pattern.compile("(?i)(?:정답|answer)[:：]\\s*([A-DOX])");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizGenerationResponse parse(String responseBody) {
        try {
            String content = extractContent(responseBody);
            return parseContent(content);
        } catch (IOException e) {
            ErrorCode.INTERNAL_SERVER_ERROR.throwServiceException(e);
            return null; // Unreachable
        }
    }

    private String extractContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
    }

    private QuizGenerationResponse parseContent(String content) {
        Map<Integer, String> questionMap = new LinkedHashMap<>();
        Map<Integer, String> answerMap = new LinkedHashMap<>();
        int currentQuestion = 0;
        StringBuilder currentQuestionText = new StringBuilder();

        for (String line : content.split("\\r?\\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            if (processQuestionLine(trimmedLine, questionMap, currentQuestionText)) {
                // 새로운 질문 라인 발견 시, 이전 질문 저장 및 현재 질문 번호 업데이트
                if (currentQuestion != 0) {
                    questionMap.put(currentQuestion, currentQuestionText.toString().trim());
                }
                Matcher qm = QUESTION_PATTERN.matcher(trimmedLine);
                qm.find(); // 반드시 매칭됨
                currentQuestion = Integer.parseInt(qm.group(1));
                currentQuestionText = new StringBuilder();
                String questionText = trimmedLine.replaceFirst("^\\d+\\.\\s*", "");
                currentQuestionText.append(questionText).append("\n");
                continue;
            }

            if (processAnswerLine(trimmedLine, answerMap, currentQuestion)) {
                continue;
            }

            // 질문 내용 추가 (질문 시작 이후의 일반 라인)
            if (currentQuestion != 0) {
                currentQuestionText.append(line).append("\n");
            }
        }

        if (currentQuestion != 0) {
            questionMap.put(currentQuestion, currentQuestionText.toString().trim());
        }

        return new QuizGenerationResponse(questionMap, answerMap);
    }

    private boolean processQuestionLine(String line, Map<Integer, String> questionMap, StringBuilder currentQuestionText) {
        return QUESTION_PATTERN.matcher(line).find();
    }

    private boolean processAnswerLine(String line, Map<Integer, String> answerMap, int currentQuestion) {
        Matcher am = ANSWER_PATTERN.matcher(line);
        if (am.find() && currentQuestion > 0) {
            answerMap.put(currentQuestion, am.group(1).toLowerCase());
            return true;
        }
        return false;
    }
}
