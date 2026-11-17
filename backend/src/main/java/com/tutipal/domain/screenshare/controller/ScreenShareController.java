package com.tutipal.domain.screenshare.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tutipal.domain.screenshare.dto.ScreenShareDtos.CreateHelpRequest;
import com.tutipal.domain.screenshare.dto.ScreenShareDtos.HelpResponse;
import com.tutipal.domain.screenshare.dto.ScreenShareDtos.JoinInfo;
import com.tutipal.domain.screenshare.service.ScreenShareService;

@RestController
@RequestMapping("/api")
public class ScreenShareController {

    private final ScreenShareService service;

    public ScreenShareController(ScreenShareService service) {
        this.service = service;
    }

    @PostMapping("/sessions/{sessionId}/screenshare/token")
    public JoinInfo classToken(@AuthenticationPrincipal OAuth2User me, @PathVariable Long sessionId) {
        return service.classToken(userId(me), sessionId);
    }

    @PostMapping("/sessions/{sessionId}/help")
    @ResponseStatus(HttpStatus.CREATED)
    public HelpResponse request(@AuthenticationPrincipal OAuth2User me, @PathVariable Long sessionId,
                                @Valid @RequestBody CreateHelpRequest body) {
        return service.request(userId(me), sessionId, body);
    }

    @GetMapping("/sessions/{sessionId}/help")
    public List<HelpResponse> list(@AuthenticationPrincipal OAuth2User me, @PathVariable Long sessionId) {
        return service.list(userId(me), sessionId);
    }

    @PostMapping("/help/{helpId}/accept")
    public HelpResponse accept(@AuthenticationPrincipal OAuth2User me, @PathVariable Long helpId) {
        return service.accept(userId(me), helpId);
    }

    @PostMapping("/help/{helpId}/later")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void later(@AuthenticationPrincipal OAuth2User me, @PathVariable Long helpId) {
        service.later(userId(me), helpId);
    }

    @PostMapping("/help/{helpId}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void end(@AuthenticationPrincipal OAuth2User me, @PathVariable Long helpId) {
        service.end(userId(me), helpId);
    }

    @PostMapping("/help/{helpId}/token")
    public JoinInfo helpToken(@AuthenticationPrincipal OAuth2User me, @PathVariable Long helpId) {
        return service.helpToken(userId(me), helpId);
    }

    private static Long userId(OAuth2User me) {
        return Long.valueOf(me.getName());
    }
}
