package com.tutipal.global.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessagingException;

import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.service.StudySessionService;

class StompAuthInterceptorTest {

    StudySessionService sessions = mock(StudySessionService.class);
    StompAuthInterceptor guard = new StompAuthInterceptor(sessions);
    Principal teacher = () -> "1";
    Principal student = () -> "2";
    Principal stranger = () -> "3";

    {
        when(sessions.roleOf(1L, 10L)).thenReturn(Optional.of(SessionRole.TEACHER));
        when(sessions.roleOf(2L, 10L)).thenReturn(Optional.of(SessionRole.STUDENT));
        when(sessions.roleOf(3L, 10L)).thenReturn(Optional.empty());
    }

    @Test
    void participantsGetEventsOnlyTeacherGetsTeacherTopic() {
        assertThatCode(() -> guard.checkSubscribe(student, "/topic/sessions/10/events")).doesNotThrowAnyException();
        assertThatCode(() -> guard.checkSubscribe(teacher, "/topic/sessions/10/teacher")).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.checkSubscribe(student, "/topic/sessions/10/teacher")).isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> guard.checkSubscribe(stranger, "/topic/sessions/10/events")).isInstanceOf(MessagingException.class);
    }

    @Test
    void rejectsOtherDestinationsAndAnonymous() {
        assertThatCode(() -> guard.checkSubscribe(student, "/user/queue/events")).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.checkSubscribe(student, "/topic/sessions/10/events/../teacher")).isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> guard.checkSubscribe(student, "/topic/everything")).isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> guard.checkSubscribe(student, "/user/2/queue/events")).isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> guard.checkSubscribe(null, "/topic/sessions/10/events")).isInstanceOf(MessagingException.class);
    }

    @Test
    void presenceOnlyCountsEventsTopic() {
        assertThat(StompAuthInterceptor.eventsTopicSession("/topic/sessions/10/events")).isEqualTo(10L);
        assertThat(StompAuthInterceptor.eventsTopicSession("/topic/sessions/10/teacher")).isNull();
    }
}
