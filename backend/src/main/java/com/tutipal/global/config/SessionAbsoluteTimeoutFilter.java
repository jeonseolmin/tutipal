package com.tutipal.global.config;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 세션 절대 제한: 계속 쓰고 있어도 만든 지 12시간이 지나면 버린다 (유휴 2시간은 spring.session.timeout).
 * 버린 뒤 요청은 비로그인으로 처리돼서 API 는 401 → 화면이 로그인으로 보낸다.
 * SecurityConfig 에서 보안 필터 체인 맨 앞에 넣는다 (Bean 이 아님: 서블릿 필터로 두 번 등록되지 않게).
 */
public class SessionAbsoluteTimeoutFilter extends OncePerRequestFilter {

    static final Duration MAX_AGE = Duration.ofHours(12);

    private final Clock clock;

    public SessionAbsoluteTimeoutFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        // changeSessionId(로그인 시 번호 교체)는 만든 시각을 유지하므로 첫 세션 기준
        if (session != null && clock.millis() - session.getCreationTime() > MAX_AGE.toMillis()) {
            session.invalidate();
        }
        chain.doFilter(request, response);
    }
}
