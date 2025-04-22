package com.ll.quizzle.global.socket.service;

import com.ll.quizzle.domain.member.service.MemberService;
import com.ll.quizzle.global.socket.dto.response.WebSocketQuizSubmitResponse;
import com.ll.quizzle.global.socket.service.quiz.RedisQuizSubmissionService;
import com.ll.quizzle.global.socket.type.RoomMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RedisQuizSubmissionServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOps;

    @Mock
    private SetOperations<String, Object> setOps;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private RedisQuizSubmissionService quizSubmissionService;

    private final String quizId = "quiz1";
    // 테스트에서는 memberId 변수를 "user1"로 사용 (원래 userId가 아니라 memberId로 취급)
    private final String memberId = "12";
    private final int questionNumber = 1;
    private final String submittedAnswer = "a";
    private final String answerKey = "quiz:" + quizId + ":answers";
    private final String currentQuestionKey = "quiz:" + quizId + ":currentQuestion";
    // 수정된 제출 키: "quiz:%s:memberId:%s:submissions"
    private final String submissionKey = String.format("quiz:%s:memberId:%s:submissions", quizId, memberId);
    private final String submittedSetKey = String.format("quiz:%s:submitted:%d", quizId, questionNumber);
    private final String participantsKey = String.format("quiz:%s:participants", quizId);

    @BeforeEach
    void setUp() {
        // 각 Operations 객체를 목(mock)으로 설정
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("정상 제출 흐름 - 올바른 WebSocketQuizSubmitResponse 반환")
    void testValidSubmissionFlow() {
        // 전체 문제 수 1, 정답 리스트에 "1:a" 저장되어 있다고 가정
        when(listOps.size(eq(answerKey))).thenReturn(1L);
        when(listOps.index(eq(answerKey), eq((long) questionNumber - 1))).thenReturn("1:a");
        // 활성 문제 번호가 없으므로 null -> 서비스 내에서 1로 초기화
        when(valueOps.get(eq(currentQuestionKey))).thenReturn(null);
        // 중복 제출 검사: 제출 키의 range는 빈 리스트 반환
        when(listOps.range(eq(submissionKey), anyLong(), anyLong())).thenReturn(Collections.emptyList());
        // 제출 Set: 아직 제출되지 않음
        when(setOps.isMember(eq(submittedSetKey), eq(memberId))).thenReturn(false);
        // 참가자 수와 제출된 인원이 모두 2명인 경우 -> 이벤트 발행 대상
        when(setOps.size(eq(participantsKey))).thenReturn(2L);
        when(setOps.size(eq(submittedSetKey))).thenReturn(2L);

        WebSocketQuizSubmitResponse response = quizSubmissionService.submitAnswer(
                quizId, memberId, questionNumber, submittedAnswer
        );

        // 채점 결과 검증 (정답 "a"를 제출하였으므로)
        assertThat(response).isNotNull();
        assertThat(response.type()).isEqualTo(RoomMessageType.ANSWER_SUBMIT);
        assertThat(response.questionNumber()).isEqualTo(questionNumber);
        assertThat(response.correct()).isTrue();
        assertThat(response.correctAnswer()).isEqualTo("a");
        // DTO에는 message 필드가 없으므로 검증하지 않음.
        assertThat(response.memberId()).isEqualTo(memberId);
        // 테스트 환경에서는 nickname을 memberId로 설정했다고 가정함.
        assertThat(response.nickname()).isEqualTo(memberId);
        assertThat(response.isSubmitted()).isTrue();
        assertThat(response.quizId()).isEqualTo(quizId);

        // 모든 참가자가 제출되었으므로, convertAndSend()가 호출되어 "quizEnd" 이벤트가 발행되어야 함.
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(redisTemplate, times(1))
                .convertAndSend(channelCaptor.capture(), eventCaptor.capture());
        assertThat(channelCaptor.getValue()).isEqualTo(String.format("quiz:%s:notifications", quizId));
        assertThat(eventCaptor.getValue()).isEqualTo("quizEnd");
    }

    @Test
    @DisplayName("잘못된 문제 번호 처리 - 문제 번호가 전체 문제 수 초과인 경우 예외 발생")
    void testInvalidQuestionNumber() {
        when(listOps.size(eq(answerKey))).thenReturn(2L);
        assertThrows(IllegalArgumentException.class, () ->
                quizSubmissionService.submitAnswer(quizId, memberId, 3, submittedAnswer)
        );
    }

    @Test
    @DisplayName("정답 형식 오류 - Redis에 저장된 정답 형식이 잘못된 경우 예외 발생")
    void testAnswerFormatError() {
        when(listOps.size(eq(answerKey))).thenReturn(1L);
        when(listOps.index(eq(answerKey), eq((long) questionNumber - 1))).thenReturn("invalidFormat");
        assertThrows(IllegalStateException.class, () ->
                quizSubmissionService.submitAnswer(quizId, memberId, questionNumber, submittedAnswer)
        );
    }

    @Test
    @DisplayName("중복 제출 방지 - 같은 사용자가 두 번 제출 시 예외 발생")
    void testDuplicateSubmission() {
        when(listOps.size(eq(answerKey))).thenReturn(1L);
        when(listOps.index(eq(answerKey), eq((long) questionNumber - 1))).thenReturn("1:a");
        when(valueOps.get(eq(currentQuestionKey))).thenReturn(1);
        // 첫 호출: 빈 리스트, 두 번째 호출: 이미 제출된 기록이 포함되어 중복 제출로 판단
        when(listOps.range(eq(submissionKey), anyLong(), anyLong()))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList("1:a:correct"));
        when(setOps.isMember(eq(submittedSetKey), eq(memberId))).thenReturn(false);
        when(setOps.size(eq(participantsKey))).thenReturn(1L);
        when(setOps.size(eq(submittedSetKey))).thenReturn(1L);

        WebSocketQuizSubmitResponse firstResponse = quizSubmissionService.submitAnswer(quizId, memberId, questionNumber, submittedAnswer);
        assertThat(firstResponse).isNotNull();

        assertThrows(IllegalStateException.class, () ->
                quizSubmissionService.submitAnswer(quizId, memberId, questionNumber, submittedAnswer)
        );
    }

    @Test
    @DisplayName("현재 활성 문제 번호 검증 - 제출 요청 문제 번호가 활성 문제 번호와 다르면 예외 발생")
    void testCurrentQuestionMismatch() {
        when(listOps.size(eq(answerKey))).thenReturn(1L);
        when(listOps.index(eq(answerKey), eq((long) questionNumber - 1))).thenReturn("1:a");
        when(valueOps.get(eq(currentQuestionKey))).thenReturn(2);
        assertThrows(IllegalStateException.class, () ->
                quizSubmissionService.submitAnswer(quizId, memberId, questionNumber, submittedAnswer)
        );
    }

    @Test
    @DisplayName("모든 참가자 제출 후 이벤트 발행 테스트 - 제출 인원과 참가자 수가 일치하면 이벤트 발행")
    void testAllParticipantsSubmittedEventEmitted() {
        when(listOps.size(eq(answerKey))).thenReturn(1L);
        when(listOps.index(eq(answerKey), eq((long) questionNumber - 1))).thenReturn("1:a");
        when(valueOps.get(eq(currentQuestionKey))).thenReturn(1);
        when(listOps.range(eq(submissionKey), anyLong(), anyLong())).thenReturn(Collections.emptyList());
        when(setOps.isMember(eq(submittedSetKey), eq(memberId))).thenReturn(false);
        when(setOps.size(eq(participantsKey))).thenReturn(3L);
        when(setOps.size(eq(submittedSetKey))).thenReturn(3L);

        WebSocketQuizSubmitResponse response = quizSubmissionService.submitAnswer(quizId, memberId, questionNumber, submittedAnswer);

        verify(redisTemplate, times(1)).convertAndSend(eq(String.format("quiz:%s:notifications", quizId)), eq("quizEnd"));
        assertThat(response).isNotNull();
        assertThat(response.questionNumber()).isEqualTo(questionNumber);
        assertThat(response.quizId()).isEqualTo(quizId);
    }
}
