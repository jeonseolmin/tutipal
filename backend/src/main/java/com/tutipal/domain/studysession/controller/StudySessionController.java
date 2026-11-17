package com.tutipal.domain.studysession.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tutipal.domain.studysession.dto.CreateSessionRequest;
import com.tutipal.domain.studysession.dto.JoinSessionRequest;
import com.tutipal.domain.studysession.dto.ParticipantResponse;
import com.tutipal.domain.studysession.dto.SessionResponse;
import com.tutipal.domain.studysession.dto.UpdateSettingsRequest;
import com.tutipal.domain.studysession.service.StudySessionService;

@RestController
@RequestMapping("/api/sessions")
public class StudySessionController {

    private final StudySessionService service;

    public StudySessionController(StudySessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@AuthenticationPrincipal OAuth2User me, @Valid @RequestBody CreateSessionRequest request) {
        return service.create(userId(me), request);
    }

    @PostMapping("/join")
    public SessionResponse join(@AuthenticationPrincipal OAuth2User me, @Valid @RequestBody JoinSessionRequest request) {
        return service.join(userId(me), request.code());
    }

    @GetMapping
    public List<SessionResponse> mySessions(@AuthenticationPrincipal OAuth2User me) {
        return service.mySessions(userId(me));
    }

    @GetMapping("/{id}")
    public SessionResponse get(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id) {
        return service.get(userId(me), id);
    }

    @GetMapping("/{id}/participants")
    public List<ParticipantResponse> participants(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id) {
        return service.participants(userId(me), id);
    }

    @PutMapping("/{id}/settings")
    public SessionResponse updateSettings(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id,
                                          @Valid @RequestBody UpdateSettingsRequest request) {
        return service.updateSettings(userId(me), id, request);
    }

    @PostMapping("/{id}/end")
    public SessionResponse end(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id) {
        return service.end(userId(me), id);
    }

    private static Long userId(OAuth2User me) {
        return Long.valueOf(me.getName());
    }
}
