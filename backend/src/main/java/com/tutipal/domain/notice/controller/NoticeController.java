package com.tutipal.domain.notice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tutipal.domain.notice.dto.NoticeDtos.CreateNotice;
import com.tutipal.domain.notice.dto.NoticeDtos.NoticeResponse;
import com.tutipal.domain.notice.service.NoticeService;

@RestController
@RequestMapping("/api")
public class NoticeController {

    private final NoticeService service;

    public NoticeController(NoticeService service) {
        this.service = service;
    }

    @PostMapping("/sessions/{sessionId}/notices")
    @ResponseStatus(HttpStatus.CREATED)
    public NoticeResponse create(@AuthenticationPrincipal OAuth2User me, @PathVariable Long sessionId, @Valid @RequestBody CreateNotice req) {
        return service.create(Long.valueOf(me.getName()), sessionId, req);
    }

    @GetMapping("/sessions/{sessionId}/notices")
    public List<NoticeResponse> list(@AuthenticationPrincipal OAuth2User me, @PathVariable Long sessionId) {
        return service.list(Long.valueOf(me.getName()), sessionId);
    }

    @DeleteMapping("/notices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id) {
        service.delete(Long.valueOf(me.getName()), id);
    }
}
