package com.tutipal.domain.auth.service;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.tutipal.domain.auth.controller.AppLoginController;

/**
 * 소셜 로그인 성공 뒤: 앱에서 시작한 로그인이면 1회용 코드를 만들어 앱으로 돌려보내고,
 * 브라우저 쪽 로그인은 버린다 (앱을 위해 연 브라우저에 로그인이 남지 않게). 아니면 웹 첫 화면으로.
 */
@Component
public class AppLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AppLoginService appLogin;

    public AppLoginSuccessHandler(AppLoginService appLogin) {
        this.appLogin = appLogin;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        HttpSession session = request.getSession(false);
        Object challenge = session == null ? null : session.getAttribute(AppLoginController.SESSION_CHALLENGE);
        if (!(challenge instanceof String c) || !AppLoginService.validChallenge(c)) {
            response.sendRedirect("/");
            return;
        }
        String code = appLogin.issue(Long.valueOf(authentication.getName()), c);
        session.invalidate();
        SecurityContextHolder.clearContext();
        response.sendRedirect("/auth/app/done?code=" + code);
    }
}
