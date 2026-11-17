package com.tutipal.domain.studysession.dto;

import java.time.Instant;

import com.tutipal.domain.studysession.entity.SessionRole;

/** online = 지금 세션 화면을 열어 둔 상태 (STOMP 구독 중, Presence). */
public record ParticipantResponse(Long userId, String name, SessionRole role, Instant joinedAt, boolean online) {
}
