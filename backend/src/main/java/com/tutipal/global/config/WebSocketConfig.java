package com.tutipal.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.tutipal.global.realtime.StompAuthInterceptor;

// 로그인 세션(Redis)으로 핸드셰이크가 인증됨: /ws 는 SecurityConfig 의 authenticated 규칙을 따름.
// 다른 사이트에서의 연결은 기본 같은 출처 검사(allowedOrigins 미설정)로 막힘.
// 구독 권한 · SEND 차단은 StompAuthInterceptor.
// ponytail: simple broker = 서버 1대 기준. 2대 이상이면 Redis pub/sub 로 브로드캐스트 추가
// ponytail: 연결 중에 세션이 만료돼도 열린 소켓은 유지됨 (알림만 받고 실제 동작은 REST 라 영향 적음)
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor authInterceptor;

    public WebSocketConfig(StompAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
