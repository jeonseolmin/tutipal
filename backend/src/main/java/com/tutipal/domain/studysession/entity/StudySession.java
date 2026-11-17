package com.tutipal.domain.studysession.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tutipal.global.common.BaseTimeEntity;

/** 단체 학습 세션. 선생님 챗봇 설정(주제 · 난이도 · 정답 숨김)이 학생 챗봇에 그대로 적용된다. */
@Entity
@Table(name = "study_sessions")
public class StudySession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    /** 과목 이름 ("코딩", "한국사", 직접 쓴 이름 등) */
    @Column(nullable = false, length = 30)
    private String topic;

    @Column(nullable = false, length = 6, unique = true)
    private String inviteCode;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int difficulty = 3;         // 1 쉬움 ~ 5 어려움

    @Column(nullable = false)
    private boolean hideAnswers;        // 정답을 바로 알려주지 않기

    @Column(nullable = false)
    private boolean preferMaterials;    // 자료 근거 우선 (ponytail: 저장만, RAG 는 5단계)

    private Instant endedAt;

    protected StudySession() {
    }

    public StudySession(String title, String topic, String inviteCode, Long ownerId) {
        this.title = title;
        this.topic = topic;
        this.inviteCode = inviteCode;
        this.ownerId = ownerId;
    }

    public void updateSettings(String topic, int difficulty, boolean hideAnswers, boolean preferMaterials) {
        this.topic = topic;
        this.difficulty = difficulty;
        this.hideAnswers = hideAnswers;
        this.preferMaterials = preferMaterials;
    }

    public void end(Instant now) {
        if (endedAt == null) {
            endedAt = now;
        }
    }

    public boolean isEnded() { return endedAt != null; }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public String getInviteCode() { return inviteCode; }
    public Long getOwnerId() { return ownerId; }
    public int getDifficulty() { return difficulty; }
    public boolean isHideAnswers() { return hideAnswers; }
    public boolean isPreferMaterials() { return preferMaterials; }
    public Instant getEndedAt() { return endedAt; }
}
