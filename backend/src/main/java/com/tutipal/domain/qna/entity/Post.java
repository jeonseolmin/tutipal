package com.tutipal.domain.qna.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.tutipal.global.common.BaseTimeEntity;

/** 게시판 글 (목업 29 오른쪽). 세션 참가자 누구나. */
@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {

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

    protected Post() {
    }

    public Post(Long sessionId, Long authorId, String title, String body) {
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
