package com.tutipal.global.config;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tutipal.domain.auth.service.SessionLogin;
import com.tutipal.domain.user.dto.SocialProfile;
import com.tutipal.domain.user.entity.User;
import com.tutipal.domain.user.service.UserService;

/**
 * 소셜 로그인 키 없이 로컬에서 테스트하기 위한 로그인. app.dev-login=true 일 때만 존재한다.
 * 운영(공개 서버)에서는 절대 켜지 말 것 — 누구나 테스트 계정으로 들어올 수 있다.
 */
@RestController
@ConditionalOnProperty(name = "app.dev-login", havingValue = "true")
public class DevLoginController {

    private final UserService userService;
    private final SessionLogin sessionLogin;

    public DevLoginController(UserService userService, SessionLogin sessionLogin) {
        this.userService = userService;
        this.sessionLogin = sessionLogin;
    }

    /** n = 1~9: 선생님 · 학생을 한 PC 에서 따로 시험하려고 계정을 여러 개 (1 은 예전과 같은 계정). */
    @PostMapping("/dev/login")
    public Map<String, String> login(@RequestParam(defaultValue = "1") int n,
                                     HttpServletRequest request, HttpServletResponse response) {
        int k = Math.clamp(n, 1, 9);
        User user = userService.upsert(k == 1
                ? new SocialProfile("dev", "dev-1", "dev@example.com", "개발 사용자")
                : new SocialProfile("dev", "dev-" + k, "dev" + k + "@example.com", "개발 사용자 " + k));
        sessionLogin.login(user, request, response);
        return Map.of("name", user.getName());
    }
}
