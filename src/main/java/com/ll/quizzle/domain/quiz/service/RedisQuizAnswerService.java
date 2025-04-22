package com.ll.quizzle.domain.quiz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RedisQuizAnswerService {

    private static final Logger log = LoggerFactory.getLogger(RedisQuizAnswerService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Duration QUIZ_TTL = Duration.ofMinutes(30);

    public String saveQuiz(String quizId, Map<Integer, String> quizTextMap, Map<Integer, String> answerMap) {
        if (quizId == null || quizId.trim().isEmpty()) {
            quizId = UUID.randomUUID().toString();
        }
        String redisKey = String.format("quiz:%s", quizId);

        String questionListKey = String.format("%s:questions", redisKey);
        quizTextMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry -> {
                    // 저장 시 "번호: 질문내용" 형식
                    String questionEntry = String.format("%d: %s\n", entry.getKey(), entry.getValue().trim());
                    redisTemplate.opsForList().rightPush(questionListKey, questionEntry);
                });
        redisTemplate.expire(questionListKey, QUIZ_TTL);

        String answerListKey = String.format("%s:answers", redisKey);
        answerMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry -> {
                    String entryValue = String.format("%d:%s", entry.getKey(), entry.getValue());
                    redisTemplate.opsForList().rightPush(answerListKey, entryValue);
                });
        redisTemplate.expire(answerListKey, QUIZ_TTL);

        return quizId;
    }

}