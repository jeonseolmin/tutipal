package com.tutipal.domain.tutor.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 도우미 챗봇 요청. 대화 기록은 화면이 들고 와서 함께 보낸다.
 * sessionId 가 있으면 topic 대신 그 세션 선생님 설정을 따른다 (참가자만).
 * ponytail: 대화는 저장하지 않음, 복습 · 기록 기능이 필요해지면 서버 저장
 */
public record ChatRequest(
        @NotBlank @Size(max = 30) String topic,
        @NotEmpty @Size(max = 40) List<@Valid Message> messages,
        Long sessionId) {

    public record Message(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 8000) String content) {
    }
}
