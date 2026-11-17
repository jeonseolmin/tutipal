package com.tutipal.domain.qna.controller;

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

import com.tutipal.domain.qna.dto.QnaDtos.CreateComment;
import com.tutipal.domain.qna.dto.QnaDtos.CreatePost;
import com.tutipal.domain.qna.dto.QnaDtos.PostDetail;
import com.tutipal.domain.qna.dto.QnaDtos.PostSummary;
import com.tutipal.domain.qna.service.QnaService;

@RestController
@RequestMapping("/api")
public class QnaController {

    private final QnaService service;

    public QnaController(QnaService service) {
        this.service = service;
    }

    @PostMapping("/sessions/{sessionId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostSummary create(@AuthenticationPrincipal OAuth2User me, @PathVariable Long sessionId, @Valid @RequestBody CreatePost req) {
        return service.createPost(uid(me), sessionId, req);
    }

    @GetMapping("/sessions/{sessionId}/posts")
    public List<PostSummary> list(@AuthenticationPrincipal OAuth2User me, @PathVariable Long sessionId) {
        return service.list(uid(me), sessionId);
    }

    @GetMapping("/posts/{id}")
    public PostDetail get(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id) {
        return service.get(uid(me), id);
    }

    @PostMapping("/posts/{id}/comments")
    public PostDetail comment(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id, @Valid @RequestBody CreateComment req) {
        return service.comment(uid(me), id, req);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id) {
        service.deletePost(uid(me), id);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id) {
        service.deleteComment(uid(me), id);
    }

    private static Long uid(OAuth2User me) {
        return Long.valueOf(me.getName());
    }
}
