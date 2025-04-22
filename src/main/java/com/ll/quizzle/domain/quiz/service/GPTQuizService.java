package com.ll.quizzle.domain.quiz.service;

import com.ll.quizzle.domain.quiz.client.OpenAIClient;
import com.ll.quizzle.domain.quiz.dto.request.QuizGenerationRequest;
import com.ll.quizzle.domain.quiz.dto.response.QuizGenerationResponse;
import com.ll.quizzle.domain.quiz.dto.response.QuizResponse;
import com.ll.quizzle.domain.quiz.parser.QuizResponseParser;
import com.ll.quizzle.domain.quiz.util.QuizPromptBuilder;
import com.ll.quizzle.global.config.OpenAIProperties;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GPTQuizService {

    private final OpenAIClient openAIClient;
    private final QuizResponseParser quizResponseParser;
    private final RedisQuizAnswerService redisQuizAnswerService;
    private final SimpMessagingTemplate messagingTemplate;

    public GPTQuizService(OpenAIProperties openAIProperties,
                          RedisQuizAnswerService redisQuizAnswerService,
                          SimpMessagingTemplate messagingTemplate) {
        this.openAIClient = new OpenAIClient(openAIProperties);
        this.quizResponseParser = new QuizResponseParser();
        this.redisQuizAnswerService = redisQuizAnswerService;
        this.messagingTemplate = messagingTemplate;
    }

    public QuizResponse generateQuiz(QuizGenerationRequest request) {
        String quizId = UUID.randomUUID().toString();

        String systemPrompt = QuizPromptBuilder.buildPrompt(request);
        String responseBody = openAIClient.sendRequest(systemPrompt, "퀴즈 생성");
        QuizGenerationResponse generationResponse = quizResponseParser.parse(responseBody);

        redisQuizAnswerService.saveQuiz(quizId, generationResponse.quizText(), generationResponse.answerMap());

        QuizResponse response = new QuizResponse(quizId, generationResponse.quizText(), generationResponse.answerMap());

        if (request.roomId() != null && !request.roomId().isEmpty()) {
            String roomId = request.roomId();
            messagingTemplate.convertAndSend("/topic/room/chat/" + roomId, response);
        }

        return response;
    }
}
