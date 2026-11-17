package com.tutipal.domain.screensummary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 목업 07 화면 요약 · 08 화면 질문. image = 학생이 고른 창 한 장 (base64 JPEG/PNG).
 * question 이 없으면 요약, 있으면 그 화면에 대한 질문. 이미지는 저장하지 않는다.
 */
public record ScreenRequest(
        @NotBlank @Size(max = 2_500_000) String image,
        @Size(max = 500) String question) {
}
