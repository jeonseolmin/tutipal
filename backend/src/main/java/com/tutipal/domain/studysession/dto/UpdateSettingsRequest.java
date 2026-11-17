package com.tutipal.domain.studysession.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 목업 21: 선생님이 정하는 학생 챗봇 설정. */
public record UpdateSettingsRequest(
        @NotBlank @Size(max = 30) String topic,
        @Min(1) @Max(5) int difficulty,
        boolean hideAnswers,
        boolean preferMaterials) {
}
