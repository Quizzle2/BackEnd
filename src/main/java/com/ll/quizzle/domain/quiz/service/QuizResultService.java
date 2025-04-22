package com.ll.quizzle.domain.quiz.service;

import com.ll.quizzle.domain.quiz.dto.response.QuizResultResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizResultService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int SCORE_PER_CORRECT = 10;

    public QuizResultService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<QuizResultResponse> getQuizResults(String quizId) {
        String pattern = String.format("quiz:%s:memberId:*:submissions", quizId);
        Set<String> keys = redisTemplate.keys(pattern);

        List<QuizResultResponse> results = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                QuizResultResponse result = processSubmissionKey(key);
                if (result != null) {
                    results.add(result);
                }
            }
        }
        return assignRanks(results);
    }

    private QuizResultResponse processSubmissionKey(String key) {
        String memberId = extractUserIdFromKey(key);
        if (memberId == null) {
            return null;
        }
        List<Object> submissions = redisTemplate.opsForList().range(key, 0, -1);
        int score = calculateScore(submissions);
        int totalQuestions = submissions != null ? submissions.size() : 0;
        int correctCount = score / SCORE_PER_CORRECT;

        // 초기 랭크는 0으로 설정
        return new QuizResultResponse(memberId, correctCount, totalQuestions, score, 0, score);
    }

    private List<QuizResultResponse> assignRanks(List<QuizResultResponse> results) {
        List<QuizResultResponse> sortedResults = results.stream()
                .sorted(Comparator.comparingInt(QuizResultResponse::score).reversed())
                .collect(Collectors.toList());

        List<QuizResultResponse> rankedResults = new ArrayList<>();
        for (int i = 0; i < sortedResults.size(); i++) {
            QuizResultResponse oldResult = sortedResults.get(i);
            QuizResultResponse newResult = new QuizResultResponse(
                    oldResult.memberId(),
                    oldResult.correctCount(),
                    oldResult.totalQuestions(),
                    oldResult.score(),
                    i + 1,
                    oldResult.exp()
            );
            rankedResults.add(newResult);
        }
        return rankedResults;
    }

    private String extractUserIdFromKey(String key) {
        String[] parts = key.split(":");
        return parts.length >= 4 ? parts[3] : null;
    }

    private int calculateScore(List<Object> submissions) {
        int correctCount = 0;
        if (submissions != null) {
            for (Object submission : submissions) {
                String[] tokens = submission.toString().split(":");
                // 유효한 제출 문자열 형식은 " 질문ID:답변:상태"
                // token[0] : 질문ID , token[1] : 제출한 답 , token[2] : 제출한 상태 ( correct)
                if (tokens.length == 3 && "correct".equals(tokens[2])) {
                    correctCount++;
                }
            }
        }
        return correctCount * SCORE_PER_CORRECT;
    }
}
