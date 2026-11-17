package com.tutipal.domain.qna.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.tutipal.global.common.BaseTimeEntity;

/** 게시판 댓글. */
@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long authorId;


    @Column(nullable = false, length = 1000)
    private String body;

    protected Comment() {
    }

    public Comment(Long postId, Long authorId, String body) {
        this.postId = postId;
        this.authorId = authorId;
        this.body = body;
    }

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public Long getAuthorId() { return authorId; }
    public String getBody() { return body; }
}
