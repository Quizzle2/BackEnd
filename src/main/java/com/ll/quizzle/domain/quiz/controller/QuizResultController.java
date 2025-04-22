package com.ll.quizzle.domain.quiz.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ll.quizzle.domain.member.service.MemberExpService;
import com.ll.quizzle.domain.quiz.dto.request.QuizScoreRequest;
import com.ll.quizzle.domain.quiz.dto.response.QuizResultResponse;
import com.ll.quizzle.domain.quiz.service.QuizResultService;
import com.ll.quizzle.global.request.Rq;
import com.ll.quizzle.global.response.RsData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Quiz Result", description = "퀴즈 결과 조회 관련 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/quiz")
public class QuizResultController {

    private final QuizResultService quizResultService;
    private final MemberExpService memberExpService;
    private final Rq rq;

    @Operation(summary = "퀴즈 결과 조회", description = "특정 퀴즈 결과를 조회하고, 각 사용자에 대해 EXP를 갱신합니다.")
    @GetMapping("/{quizId}/result")
    public RsData<List<QuizResultResponse>> getQuizResults(@PathVariable("quizId") String quizId) {
        List<QuizResultResponse> results = quizResultService.getQuizResults(quizId);
        results.forEach(result -> memberExpService.updateMemberExp(Long.parseLong(result.memberId()), result.score()));

        return RsData.success(HttpStatus.OK, results);
    }

    @Operation(summary = "퀴즈 점수 업데이트", description = "현재 로그인한 사용자의 퀴즈 점수를 경험치로 업데이트합니다.")
    @PostMapping("/{quizId}/result")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateQuizScore(@PathVariable("quizId") String quizId, @RequestBody QuizScoreRequest request) {
        Long memberId = rq.getActor().getId();

        memberExpService.updateMemberExp(memberId, request.score());
        }
}
