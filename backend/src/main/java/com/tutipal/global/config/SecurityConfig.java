package com.tutipal.global.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.tutipal.domain.auth.service.AppLoginSuccessHandler;
import com.tutipal.domain.user.service.SocialOAuth2UserService;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SocialOAuth2UserService socialUserService,
                                    AppLoginSuccessHandler appLoginSuccess) throws Exception {
        http
            .addFilterBefore(new SessionAbsoluteTimeoutFilter(Clock.systemUTC()), SecurityContextHolderFilter.class)
            // Spring 이 React 화면도 직접 준다(nginx 없음): 화면 파일·주소는 누구나, 데이터(API)·실시간 연결만 로그인 필요.
            // 화면은 /api/users/me 로 로그인 여부를 확인해서 비로그인이면 /login 화면으로 보낸다.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/app/exchange").permitAll()   // 앱 딥링크 로그인 교환 (verifier 로 보호)
                .requestMatchers("/api/**", "/ws/**").authenticated()
                .anyRequest().permitAll())   // 화면·health·/dev/login(app.dev-login=true 일 때만 존재)
            // React: XSRF-TOKEN 쿠키 → X-XSRF-TOKEN 헤더
            // 앱 교환 요청은 아직 쿠키가 없는 Electron 메인 프로세스가 보냄 → CSRF 대신 1회용 코드 + verifier 로 보호
            .csrf(csrf -> csrf.spa().ignoringRequestMatchers("/api/auth/app/exchange"))
            // API 는 로그인 페이지로 리다이렉트 대신 401 (React 가 로그인 버튼을 보여줌)
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiRequests()))
            // /oauth2/authorization/{google|naver|kakao} 로 로그인 시작, 끝나면 React 첫 화면 (앱에서 시작했으면 딥링크로)
            .oauth2Login(oauth -> oauth
                // 로그인 화면은 우리 React /login (Spring 기본 로그인 페이지를 만들지 않게)
                .loginPage("/login")
                .userInfoEndpoint(info -> info.userService(socialUserService))  // 소셜 사용자 → users 테이블
                .successHandler(appLoginSuccess))
            .logout(logout -> logout.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));
        return http.build();
    }

    private static RequestMatcher apiRequests() {
        return request -> request.getRequestURI().startsWith("/api/");
    }
}
