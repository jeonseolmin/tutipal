package com.tutipal.domain.studysession.entity;

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

/** 세션 참가자 한 명 (참여 시각 = createdAt). 사용자 정보는 user 도메인 Service 로 조회. */
@Entity
@Table(name = "session_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"sessionId", "userId"}))
public class SessionParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionRole role;

    /** 홈 목록에서 즐겨찾기 (맨 위 고정) */
    @Column(nullable = false)
    private boolean pinned;

    protected SessionParticipant() {
    }

    public SessionParticipant(Long sessionId, Long userId, SessionRole role) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.role = role;
    }

    public boolean isTeacher() { return role == SessionRole.TEACHER; }
    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getUserId() { return userId; }
    public SessionRole getRole() { return role; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}
