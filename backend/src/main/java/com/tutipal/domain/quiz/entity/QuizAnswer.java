package com.tutipal.domain.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.tutipal.global.common.BaseTimeEntity;

/** 학생 응답 한 개. gradedBy == null 이면 아직 채점 전 (AI 채점 중 또는 실패 → 선생님이 채점). */
@Entity
@Table(name = "quiz_answers", uniqueConstraints = @UniqueConstraint(columnNames = {"quizId", "userId"}))
public class QuizAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long quizId;

    @Column(nullable = false)
    private Long userId;

    private Integer choiceIndex;

    @Column(length = 2000)
    private String text;

    private Boolean correct;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GradedBy gradedBy;

    @Column(length = 500)
    private String feedback;

    protected QuizAnswer() {
    }

    public QuizAnswer(Long quizId, Long userId, Integer choiceIndex, String text) {
        this.quizId = quizId;
        this.userId = userId;
        this.choiceIndex = choiceIndex;
        this.text = text;
    }

    public void grade(boolean correct, GradedBy by, String feedback) {
        this.correct = correct;
        this.gradedBy = by;
        this.feedback = feedback;
    }

    /** AI 채점 실패: 채점 전으로 두고 안내만 남김 (선생님이 채점). */
    public void gradingFailed(String note) {
        this.correct = null;
        this.gradedBy = null;
        this.feedback = note;
    }

    public boolean isPending() { return gradedBy == null; }
    public Long getId() { return id; }
    public Long getQuizId() { return quizId; }
    public Long getUserId() { return userId; }
    public Integer getChoiceIndex() { return choiceIndex; }
    public String getText() { return text; }
    public Boolean getCorrect() { return correct; }
    public GradedBy getGradedBy() { return gradedBy; }
    public String getFeedback() { return feedback; }
}
