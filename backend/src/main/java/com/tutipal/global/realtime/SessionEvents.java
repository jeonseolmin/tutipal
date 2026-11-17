package com.tutipal.global.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.tutipal.global.common.AfterCommit;

/**
 * STOMP 로 보내는 실시간 알림. 내용은 "무엇이 바뀌었나(type, id)" 만 보내고, 화면이 REST 로 다시 읽는다
 * (권한 검사를 REST 한 곳에서만 하려고).
 * <ul>
 *   <li>/topic/sessions/{id}/events: 세션 참가자 전체 (quiz, quizClosed, settings, ended)</li>
 *   <li>/topic/sessions/{id}/teacher: 그 세션 선생님만 (joined, presence, quizStats)</li>
 *   <li>/user/queue/events: 한 사람에게 (quizResult)</li>
 * </ul>
 */
@Component
public class SessionEvents {

    public record Event(String type, Long id) {
    }

    private final SimpMessagingTemplate broker;

    public SessionEvents(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    public void toSession(Long sessionId, String type, Long id) {
        send("/topic/sessions/" + sessionId + "/events", new Event(type, id));
    }

    public void toTeacher(Long sessionId, String type, Long id) {
        send("/topic/sessions/" + sessionId + "/teacher", new Event(type, id));
    }

    public void toUser(Long userId, String type, Long id) {
        Event event = new Event(type, id);
        AfterCommit.run(() -> broker.convertAndSendToUser(String.valueOf(userId), "/queue/events", event));
    }

    private void send(String destination, Event event) {
        AfterCommit.run(() -> broker.convertAndSend(destination, event));
    }
}
