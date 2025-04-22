package com.ll.quizzle.global.socket.controller;

import com.ll.quizzle.global.socket.dto.request.WebSocketQuizSubmitRequest;
import com.ll.quizzle.global.socket.dto.response.WebSocketQuizSubmitResponse;
import com.ll.quizzle.global.socket.service.quiz.RedisQuizSubmissionService;
import com.ll.quizzle.global.security.oauth2.dto.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Objects;

@Slf4j
@Controller
public class WebSocketQuizController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisQuizSubmissionService quizSubmissionService;

    public WebSocketQuizController(SimpMessagingTemplate messagingTemplate,
                                   RedisQuizSubmissionService quizSubmissionService) {
        this.messagingTemplate = messagingTemplate;
        this.quizSubmissionService = quizSubmissionService;
    }

    @MessageMapping("/quiz/{quizId}/submit")
    public void handleQuizSubmission(@DestinationVariable String quizId,
                                     @Payload WebSocketQuizSubmitRequest submitRequest,
                                     SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        String username = Objects.requireNonNull(principal).getName();

        String senderId = username;
        if (principal instanceof Authentication auth) {
            Object userObj = auth.getPrincipal();
            if (userObj instanceof SecurityUser securityUser) {
                senderId = String.valueOf(securityUser.getId());
            }
        }

        log.debug("퀴즈 제출 요청 수신 - quizId: {}, 전송자: {}, 요청 내용: {}",
                quizId, senderId, submitRequest);

        WebSocketQuizSubmitResponse response = quizSubmissionService.submitAnswer(
                quizId,
                senderId,
                submitRequest.questionNumber(),
                submitRequest.submittedAnswer()
        );

        messagingTemplate.convertAndSend("/topic/quiz/" + quizId + "/updates", response);
    }
}
