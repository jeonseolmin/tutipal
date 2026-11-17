package com.tutipal.domain.quiz.entity;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tutipal.global.common.BaseTimeEntity;

/** 선생님이 세션에 보낸 이해도 질문. closesAt 이 지나면 응답을 받지 않는다 (일찍 닫기 = closesAt 을 지금으로). */
@Entity
@Table(name = "quizzes")
public class Quiz extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizType type;

    @Column(nullable = false, length = 1000)
    private String question;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> choices;

    private Integer answerIndex;

    @Column(length = 2000)
    private String modelAnswer;

    @Column(nullable = false)
    private Instant closesAt;

    protected Quiz() {
    }

    public Quiz(Long sessionId, QuizType type, String question, List<String> choices, Integer answerIndex,
                String modelAnswer, Instant closesAt) {
        this.sessionId = sessionId;
        this.type = type;
        this.question = question;
        this.choices = choices;
        this.answerIndex = answerIndex;
        this.modelAnswer = modelAnswer;
        this.closesAt = closesAt;
    }

    public boolean isOpen(Instant now) {
        return now.isBefore(closesAt);
    }

    public void close(Instant now) {
        if (isOpen(now)) {
            closesAt = now;
        }
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public QuizType getType() { return type; }
    public String getQuestion() { return question; }
    public List<String> getChoices() { return choices; }
    public Integer getAnswerIndex() { return answerIndex; }
    public String getModelAnswer() { return modelAnswer; }
    public Instant getClosesAt() { return closesAt; }
}
