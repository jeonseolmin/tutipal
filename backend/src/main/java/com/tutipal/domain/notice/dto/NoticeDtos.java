package com.tutipal.domain.notice.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class NoticeDtos {

    private NoticeDtos() {
    }

    /** alert = 학생 화면에 바로 알림 (목업 29 "학생에게 알림") */
    public record CreateNotice(@NotBlank @Size(max = 100) String title, @NotBlank @Size(max = 3000) String body, boolean alert) {
    }

    public record NoticeResponse(Long id, String title, String body, String authorName, Instant createdAt) {
    }
}
