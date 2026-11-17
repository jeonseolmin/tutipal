package com.tutipal.domain.screenshare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.tutipal.global.common.BaseTimeEntity;

/** 학생의 도움 요청. PENDING(대기) → ACCEPTED(1:1 화면공유 중) → DONE, 또는 DECLINED(나중에). */
@Entity
@Table(name = "help_requests")
public class HelpRequest extends BaseTimeEntity {

    public enum Status { PENDING, ACCEPTED, DECLINED, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(nullable = false)
    private boolean shareScreen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    protected HelpRequest() {
    }

    public HelpRequest(Long sessionId, Long userId, String message, boolean shareScreen) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.message = message;
        this.shareScreen = shareScreen;
    }

    public boolean isOpen() {
        return status == Status.PENDING || status == Status.ACCEPTED;
    }

    public void moveTo(Status next) {
        this.status = next;
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getUserId() { return userId; }
    public String getMessage() { return message; }
    public boolean isShareScreen() { return shareScreen; }
    public Status getStatus() { return status; }
}
