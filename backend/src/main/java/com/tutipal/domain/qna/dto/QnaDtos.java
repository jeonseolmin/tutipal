package com.tutipal.domain.qna.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class QnaDtos {

    private QnaDtos() {
    }

    public record CreatePost(@NotBlank @Size(max = 100) String title, @NotBlank @Size(max = 3000) String body) {
    }

    public record CreateComment(@NotBlank @Size(max = 1000) String body) {
    }

    /** 목록: 댓글 수만 */
    public record PostSummary(Long id, String title, String authorName, long commentCount, Instant createdAt, boolean mine) {
    }

    public record CommentResponse(Long id, String body, String authorName, Instant createdAt, boolean mine) {
    }

    public record PostDetail(Long id, String title, String body, String authorName, Instant createdAt, boolean mine,
                             List<CommentResponse> comments) {
    }
}
