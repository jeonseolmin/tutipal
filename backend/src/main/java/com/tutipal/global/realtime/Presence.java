package com.tutipal.global.realtime;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * 접속 중 표시(목업 20 초록 점): 세션 events 토픽을 구독 중인 사람 = 접속 중.
 * ponytail: 서버 1대 메모리 기준, 세션 · 사람 수가 적어 전체 훑기. 서버가 늘면 Redis 로
 */
@Component
public class Presence {

    private record Entry(Long sessionId, Long userId) {
    }

    /** key = WebSocket 연결 id + "/" + 구독 id */
    private final Map<String, Entry> subscriptions = new ConcurrentHashMap<>();
    private final SessionEvents events;

    public Presence(SessionEvents events) {
        this.events = events;
    }

    public Set<Long> onlineUsers(Long sessionId) {
        return subscriptions.values().stream()
                .filter(e -> e.sessionId().equals(sessionId))
                .map(Entry::userId)
                .collect(Collectors.toSet());
    }

    @EventListener
    void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor h = StompHeaderAccessor.wrap(event.getMessage());
        Long sessionId = StompAuthInterceptor.eventsTopicSession(h.getDestination());
        if (sessionId == null || event.getUser() == null) {
            return;
        }
        Long userId = Long.valueOf(event.getUser().getName());
        subscriptions.put(h.getSessionId() + "/" + h.getSubscriptionId(), new Entry(sessionId, userId));
        events.toTeacher(sessionId, "presence", userId);
    }

    @EventListener
    void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor h = StompHeaderAccessor.wrap(event.getMessage());
        Entry gone = subscriptions.remove(h.getSessionId() + "/" + h.getSubscriptionId());
        if (gone != null) {
            events.toTeacher(gone.sessionId(), "presence", gone.userId());
        }
    }

    @EventListener
    void onDisconnect(SessionDisconnectEvent event) {
        String prefix = event.getSessionId() + "/";
        subscriptions.entrySet().removeIf(e -> {
            if (e.getKey().startsWith(prefix)) {
                events.toTeacher(e.getValue().sessionId(), "presence", e.getValue().userId());
                return true;
            }
            return false;
        });
    }
}
