package com.tutipal.domain.auth.controller;

import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.tutipal.domain.auth.service.AppLoginService;
import com.tutipal.domain.auth.service.SessionLogin;
import com.tutipal.domain.user.dto.SocialProfile;
import com.tutipal.domain.user.entity.User;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.exception.ApiException;

/**
 * 앱 로그인 (목업 02 "브라우저에서 로그인 중…").
 * 1) 앱이 시스템 브라우저로 /auth/app/start?provider=&challenge= 를 연다
 * 2) 소셜 로그인 성공 → AppLoginSuccessHandler 가 1회용 코드를 만들어 /auth/app/done?code= (React) → tutipal://auth?code=
 * 3) 앱이 POST /api/auth/app/exchange {code, verifier} → 앱 쪽 세션 쿠키 발급
 */
@RestController
public class AppLoginController {

    /** 브라우저 세션에 잠깐 들고 있는 앱의 challenge (로그인 성공 처리에서 꺼냄). */
    public static final String SESSION_CHALLENGE = "tutipal.appLoginChallenge";
    private static final Set<String> PROVIDERS = Set.of("google", "naver", "kakao");

    public record ExchangeRequest(@NotBlank @Size(max = 64) String code, @NotBlank @Size(max = 128) String verifier) {
    }

    private final AppLoginService appLogin;
    private final SessionLogin sessionLogin;
    private final UserService userService;
    private final boolean devLogin;

    public AppLoginController(AppLoginService appLogin, SessionLogin sessionLogin, UserService userService,
                              @Value("${app.dev-login:false}") boolean devLogin) {
        this.appLogin = appLogin;
        this.sessionLogin = sessionLogin;
        this.userService = userService;
        this.devLogin = devLogin;
    }

    @GetMapping("/auth/app/start")
    public RedirectView start(@RequestParam String provider, @RequestParam String challenge, HttpServletRequest request) {
        if (!AppLoginService.validChallenge(challenge)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "잘못된 로그인 요청이에요. 앱에서 다시 시도해 주세요");
        }
        if (devLogin && provider.equals("dev")) {
            // 개발용: 소셜 키 없이 딥링크 흐름 전체를 시험 (app.dev-login=true 일 때만)
            User user = userService.upsert(new SocialProfile("dev", "dev-1", "dev@example.com", "개발 사용자"));
            return new RedirectView("/auth/app/done?code=" + appLogin.issue(user.getId(), challenge));
        }
        if (!PROVIDERS.contains(provider)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인이에요");
        }
        request.getSession(true).setAttribute(SESSION_CHALLENGE, challenge);
        return new RedirectView("/oauth2/authorization/" + provider);
    }

    @PostMapping("/api/auth/app/exchange")
    public Map<String, String> exchange(@Valid @RequestBody ExchangeRequest body,
                                        HttpServletRequest request, HttpServletResponse response) {
        Long userId = appLogin.exchange(body.code(), body.verifier());
        User user = userService.getUser(userId);
        sessionLogin.login(user, request, response);
        return Map.of("name", user.getName());
    }
}
