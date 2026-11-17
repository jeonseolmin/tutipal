package com.tutipal.domain.studysession.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.tutipal.domain.studysession.dto.UpdateSettingsRequest;
import com.tutipal.domain.studysession.entity.SessionParticipant;
import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.entity.StudySession;
import com.tutipal.domain.studysession.repository.SessionParticipantRepository;
import com.tutipal.domain.studysession.repository.StudySessionRepository;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.exception.ApiException;
import com.tutipal.global.realtime.Presence;
import com.tutipal.global.realtime.SessionEvents;

class StudySessionServiceTest {

    static final long TEACHER = 1L, STUDENT = 2L, STRANGER = 3L, SID = 10L;

    StudySessionRepository sessions = mock(StudySessionRepository.class);
    SessionParticipantRepository participants = mock(SessionParticipantRepository.class);
    StudySessionService service = new StudySessionService(sessions, participants, mock(UserService.class),
            mock(SessionEvents.class), mock(Presence.class));
    StudySession session;

    @BeforeEach
    void setUp() {
        session = new StudySession("자바 기초", "코딩", "A7K2Q9", TEACHER);
        ReflectionTestUtils.setField(session, "id", SID);
        when(sessions.findById(SID)).thenReturn(Optional.of(session));
        when(sessions.findByInviteCode("A7K2Q9")).thenReturn(Optional.of(session));
        when(participants.findBySessionIdAndUserId(SID, TEACHER))
                .thenReturn(Optional.of(new SessionParticipant(SID, TEACHER, SessionRole.TEACHER)));
        when(participants.findBySessionIdAndUserId(SID, STUDENT))
                .thenReturn(Optional.of(new SessionParticipant(SID, STUDENT, SessionRole.STUDENT)));
        when(participants.findBySessionIdAndUserId(SID, STRANGER)).thenReturn(Optional.empty());
        when(participants.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static HttpStatus status(Runnable call) {
        try {
            call.run();
        } catch (ApiException e) {
            return e.getStatus();
        }
        throw new AssertionError("ApiException 이 나야 함");
    }

    @Test
    void joinWithMessyCodeMakesStudent() {
        assertThat(service.join(STRANGER, " a7k-2q9 ").myRole()).isEqualTo(SessionRole.STUDENT);
        verify(participants).save(any());
    }

    @Test
    void teacherJoiningOwnSessionStaysTeacher() {
        assertThat(service.join(TEACHER, "A7K2Q9").myRole()).isEqualTo(SessionRole.TEACHER);
        verify(participants, never()).save(any());
    }

    @Test
    void joinRejectsUnknownAndEndedSessions() {
        when(sessions.findByInviteCode("ZZZZZZ")).thenReturn(Optional.empty());
        assertThat(status(() -> service.join(STRANGER, "ZZZZZZ"))).isEqualTo(HttpStatus.NOT_FOUND);
        session.end(Instant.now());
        assertThat(status(() -> service.join(STRANGER, "A7K2Q9"))).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void onlyTeacherChangesSettingsAndStrangerSeesNothing() {
        var req = new UpdateSettingsRequest("수학", 5, true, false);
        assertThat(status(() -> service.updateSettings(STUDENT, SID, req))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(() -> service.updateSettings(STRANGER, SID, req))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(() -> service.participants(STUDENT, SID))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(() -> service.get(STRANGER, SID))).isEqualTo(HttpStatus.NOT_FOUND);

        service.updateSettings(TEACHER, SID, req);
        var settings = service.chatSettings(STUDENT, SID);
        assertThat(settings.topic()).isEqualTo("수학");
        assertThat(settings.difficulty()).isEqualTo(5);
        assertThat(settings.hideAnswers()).isTrue();
    }

    @Test
    void chatSettingsBlockedForStrangerAndEndedSession() {
        assertThat(status(() -> service.chatSettings(STRANGER, SID))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status(() -> service.end(STUDENT, SID))).isEqualTo(HttpStatus.FORBIDDEN);
        service.end(TEACHER, SID);
        assertThatThrownBy(() -> service.chatSettings(STUDENT, SID)).isInstanceOf(ApiException.class);
        assertThat(status(() -> service.chatSettings(STUDENT, SID))).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void inviteCodesUseOnlyUnambiguousCharacters() {
        for (int i = 0; i < 200; i++) {
            assertThat(InviteCodes.generate()).matches("[" + InviteCodes.ALPHABET + "]{6}");
        }
    }
}
