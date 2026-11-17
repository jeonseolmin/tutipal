package com.tutipal.domain.studysession.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank @Size(max = 50) String title,
        @NotBlank @Size(max = 20) String topic) {
}
