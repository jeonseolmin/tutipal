package com.tutipal.global.realtime;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.service.StudySessionService;

/**
 * STOMP 권한: 연결 자체는 로그인 세션 쿠키로 인증된 /ws 핸드셰이크 (SecurityConfig).
 * 여기서는 구독할 수 있는 곳만 제한한다. 세션 토픽은 참가자만, teacher 토픽은 그 세션 선생님만.
 * 화면에서 서버로 보내는 SEND 는 막는다 (보낼 일은 REST 로. 안 막으면 아무나 토픽에 가짜 알림을 뿌릴 수 있음).
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Pattern SESSION_TOPIC = Pattern.compile("^/topic/sessions/(\\d{1,18})/(events|teacher)$");

    private final StudySessionService sessions;

    // @Lazy: 세션 서비스 → SessionEvents → 메시징 설정 → 이 인터셉터 로 이어지는 빈 순환을 끊음
    public StompAuthInterceptor(@Lazy StudySessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor h = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (h == null || h.getCommand() == null) {
            return message;
        }
        if (h.getCommand() == StompCommand.SEND) {
            throw new MessagingException("SEND 는 허용하지 않음");
        }
        if (h.getCommand() == StompCommand.SUBSCRIBE) {
            checkSubscribe(h.getUser(), h.getDestination());
        }
        return message;
    }

    void checkSubscribe(Principal user, String destination) {
        if (user == null || destination == null) {
            throw new MessagingException("로그인 필요");
        }
        if (destination.equals("/user/queue/events")) {
            return;   // 자기 앞으로 온 것만 받음 (Spring 이 사용자별로 나눔)
        }
        Matcher m = SESSION_TOPIC.matcher(destination);
        if (!m.matches()) {
            throw new MessagingException("구독할 수 없는 곳");
        }
        SessionRole role = sessions.roleOf(Long.valueOf(user.getName()), Long.valueOf(m.group(1)))
                .orElseThrow(() -> new MessagingException("세션 참가자 아님"));
        if (m.group(2).equals("teacher") && role != SessionRole.TEACHER) {
            throw new MessagingException("선생님 전용");
        }
    }

    /** /topic/sessions/{id}/events 면 id, 아니면 null (Presence 용). */
    static Long eventsTopicSession(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher m = SESSION_TOPIC.matcher(destination);
        return m.matches() && m.group(2).equals("events") ? Long.valueOf(m.group(1)) : null;
    }
}
