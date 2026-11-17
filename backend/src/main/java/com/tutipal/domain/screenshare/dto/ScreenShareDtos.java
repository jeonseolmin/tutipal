package com.tutipal.domain.screenshare.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.tutipal.domain.screenshare.entity.HelpRequest;

/** 화면공유 · 도움 요청 요청/응답 (화면 api/types.ts 와 1:1). */
public final class ScreenShareDtos {

    private ScreenShareDtos() {
    }

    /** LiveKit 접속 정보. url 로 접속해 token 으로 입장. */
    public record JoinInfo(String url, String token, String room, String identity) {
    }

    /** 목업 17 */
    public record CreateHelpRequest(@NotBlank @Size(max = 300) String message, boolean shareScreen) {
    }

    /** 목업 25: 대기 시간은 createdAt 으로 화면이 계산 */
    public record HelpResponse(Long id, Long userId, String name, String message, boolean shareScreen,
                               HelpRequest.Status status, Instant createdAt) {
    }
}
