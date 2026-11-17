package com.tutipal.domain.quiz.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.tutipal.domain.quiz.entity.GradedBy;
import com.tutipal.domain.quiz.entity.QuizType;

/** 이해도 질문 요청/응답 모양 (화면 api/types.ts 와 1:1). 작은 record 들이라 한 파일에 모음. */
public final class QuizDtos {

    private QuizDtos() {
    }

    /** 목업 22: 선생님이 만드는 질문. AI 로 만든 초안(QuizDraft)도 같은 모양으로 고쳐서 보낸다. */
    public record CreateQuizRequest(
            @NotNull QuizType type,
            @NotBlank @Size(max = 1000) String question,
            @Size(max = 5) List<@NotBlank @Size(max = 200) String> choices,
            Integer answerIndex,
            @Size(max = 2000) String modelAnswer,
            @Min(10) @Max(600) Integer timeLimitSec) {
    }

    public record GenerateQuizRequest(@NotNull QuizType type, @Size(max = 200) String hint) {
    }

    /** AI 가 만든 질문 초안 (저장 안 함, 선생님이 고쳐서 CreateQuizRequest 로 보냄). */
    public record QuizDraft(QuizType type, String question, List<String> choices, Integer answerIndex,
                            String modelAnswer) {
    }

    public record AnswerRequest(Integer choiceIndex, @Size(max = 2000) String text) {
    }

    public record GradeRequest(@NotNull Boolean correct) {
    }

    /** 내 응답 결과 (목업 16). gradedBy == null 이면 채점 중. */
    public record MyAnswer(Integer choiceIndex, String text, Boolean correct, GradedBy gradedBy, String feedback) {
    }

    /** 학생이 보는 질문 (정답 · 모범 답안 없음, 목업 15). */
    public record StudentQuiz(Long id, Long sessionId, QuizType type, String question, List<String> choices,
                              Instant closesAt, boolean open, MyAnswer myAnswer) {
    }

    /** 선생님이 보는 질문 + 실시간 정답률 (목업 23). accuracy 분모 = 채점된 응답 수. */
    public record QuizSummary(Long id, int no, QuizType type, String question, List<String> choices,
                              Integer answerIndex, String modelAnswer, Instant closesAt, boolean open,
                              long answered, long correct, long pending, long studentCount) {
    }

    /** 목업 24: 응답 하나 (선생님용). */
    public record AnswerRow(Long answerId, Long userId, String name, Integer choiceIndex, String text,
                            Boolean correct, GradedBy gradedBy, String feedback) {
    }
}
