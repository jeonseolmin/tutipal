package com.tutipal.domain.tutor.controller;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tutipal.domain.tutor.dto.ChatRequest;
import com.tutipal.domain.tutor.service.TutorService;
import com.tutipal.global.ai.AiUnavailableException;
import com.tutipal.global.exception.ApiException;

@RestController
@RequestMapping("/api/tutor")
public class TutorController {

    private static final long TIMEOUT_MS = 10 * 60 * 1000L;
    private static final long PING_MS = 15_000L;

    /** 화면이 연결을 끊었거나(중지 · 창 닫기) 시간이 다 됨 → 더 보낼 곳이 없으니 생성을 멈춘다. */
    private static final class ClientGone extends RuntimeException {
        ClientGone() {
            super(null, null, false, false);
        }
    }

    private final TutorService tutor;

    public TutorController(TutorService tutor) {
        this.tutor = tutor;
    }

    /** SSE 이벤트: (자료를 찾았으면) sources {"items": [출처]} → delta {"t": 조각} 여러 번 → done, 실패하면 error {"message": …}. */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@AuthenticationPrincipal OAuth2User me, @Valid @RequestBody ChatRequest request) {
        Long userId = Long.valueOf(me.getName());
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        AtomicBoolean gone = new AtomicBoolean(false);
        emitter.onTimeout(() -> gone.set(true));
        emitter.onError(e -> gone.set(true));
        emitter.onCompletion(() -> gone.set(true));

        // 답변이 끝날 때까지 기다리는 일이라 가상 스레드 하나에 맡긴다
        Thread.ofVirtual().name("tutor-chat").start(() -> {
            try {
                tutor.chat(userId, request, sources -> send(emitter, "sources", Map.of("items", sources)), delta -> {
                    if (gone.get()) {
                        throw new ClientGone();       // 다음 조각에서 멈춤 → AI 스트림 닫힘 → GPU 자리 반납
                    }
                    send(emitter, "delta", Map.of("t", delta));
                });
                send(emitter, "done", Map.of());
                emitter.complete();
            } catch (ClientGone e) {
                quietComplete(emitter);
            } catch (ApiException | AiUnavailableException e) {
                finishWithError(emitter, e.getMessage());
            } catch (RuntimeException e) {
                finishWithError(emitter, "답변 중 문제가 생겼어요");
            }
        });

        // 모델을 처음 올리는 동안 조각이 한참 안 올 수 있음 → 주기적으로 주석 한 줄(": ping")을 보내
        // Cloudflare Tunnel(100초 무응답 시 끊음) 같은 중간 장치가 연결을 닫지 않게 한다. 화면은 주석을 무시.
        Thread.ofVirtual().name("tutor-ping").start(() -> {
            while (!gone.get()) {
                try {
                    Thread.sleep(PING_MS);
                    if (!gone.get()) {
                        emitter.send(SseEmitter.event().comment("ping"));
                    }
                } catch (InterruptedException e) {
                    return;
                } catch (IOException | IllegalStateException e) {
                    gone.set(true);                    // 보낼 수 없으면 화면이 떠난 것 → 생성도 멈춤
                }
            }
        });
        return emitter;
    }

    private static void finishWithError(SseEmitter emitter, String message) {
        try {
            send(emitter, "error", Map.of("message", message));
            emitter.complete();
        } catch (ClientGone e) {
            quietComplete(emitter);
        }
    }

    private static void quietComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // 이미 끝난 응답
        }
    }

    /** 끊긴 연결에 보내면 IOException, 이미 끝난 emitter 면 IllegalStateException → 둘 다 ClientGone */
    private static void send(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            throw new ClientGone();
        }
    }
}
