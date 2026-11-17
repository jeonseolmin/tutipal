package com.tutipal.domain.notice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.tutipal.global.common.BaseTimeEntity;

/** 세션 공지 (목업 29 왼쪽). 선생님만 쓴다. */
@Entity
@Table(name = "notices")
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 3000)
    private String body;

    protected Notice() {
    }

    public Notice(Long sessionId, Long authorId, String title, String body) {
        this.sessionId = sessionId;
        this.authorId = authorId;
        this.title = title;
        this.body = body;
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public Long getAuthorId() { return authorId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
}
