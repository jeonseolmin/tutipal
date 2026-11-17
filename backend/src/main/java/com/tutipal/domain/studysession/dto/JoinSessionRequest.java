package com.tutipal.domain.studysession.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinSessionRequest(@NotBlank @Size(max = 20) String code) {
}
