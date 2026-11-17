package com.tutipal.domain.review.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tutipal.domain.review.repository.ReviewStore.Item;
import com.tutipal.domain.review.service.ReviewService;
import com.tutipal.domain.review.service.ReviewService.Observed;

/** 복습: 자기 기록만 (모든 요청이 로그인한 본인 id 기준). */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    /** summary 가 없으면 같은 화면을 계속 보는 중 */
    public record ObserveRequest(@Size(max = 2000) String summary, int seconds) {
    }

    public record ConsentRequest(boolean on) {
    }

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @GetMapping("/consent")
    public Map<String, Boolean> consent(@AuthenticationPrincipal OAuth2User me) {
        return Map.of("on", service.consent(uid(me)));
    }

    @PutMapping("/consent")
    public Map<String, Boolean> setConsent(@AuthenticationPrincipal OAuth2User me, @RequestBody ConsentRequest req) {
        service.setConsent(uid(me), req.on());
        return Map.of("on", req.on());
    }

    @PostMapping("/observe")
    public Observed observe(@AuthenticationPrincipal OAuth2User me, @Valid @RequestBody ObserveRequest req) {
        return service.observe(uid(me), req.summary(), req.seconds());
    }

    @GetMapping("/items")
    public List<Item> list(@AuthenticationPrincipal OAuth2User me) {
        return service.list(uid(me));
    }

    @GetMapping("/items/{id}")
    public Item get(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id) {
        return service.get(uid(me), id);
    }

    @DeleteMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll(@AuthenticationPrincipal OAuth2User me) {
        service.deleteAll(uid(me));
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal OAuth2User me, @PathVariable Long id) {
        service.delete(uid(me), id);
    }

    private static Long uid(OAuth2User me) {
        return Long.valueOf(me.getName());
    }
}
