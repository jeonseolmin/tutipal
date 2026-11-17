package com.tutipal.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import com.tutipal.domain.user.entity.User;
import com.tutipal.domain.user.service.SocialOAuth2UserService;

/**
 * 소셜 로그인 화면을 거치지 않고 세션 쿠키 로그인을 만든다 (앱 딥링크 교환 · 개발용 로그인).
 * 세션에 넣는 모양은 소셜 로그인과 같다 (principal.getName() == users.id).
 */
@Component
public class SessionLogin {

    private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public void login(User user, HttpServletRequest request, HttpServletResponse response) {
        OAuth2User principal = SocialOAuth2UserService.principalOf(user);
        request.getSession(true);
        request.changeSessionId();   // 로그인 전 세션 번호를 재사용하지 않게 (세션 고정 방지)
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
