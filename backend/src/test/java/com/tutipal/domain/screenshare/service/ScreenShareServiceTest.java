package com.tutipal.domain.screenshare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.tutipal.domain.screenshare.dto.ScreenShareDtos.CreateHelpRequest;
import com.tutipal.domain.screenshare.entity.HelpRequest;
import com.tutipal.domain.screenshare.entity.HelpRequest.Status;
import com.tutipal.domain.screenshare.repository.HelpRequestRepository;
import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.service.StudySessionService;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.exception.ApiException;
import com.tutipal.global.realtime.SessionEvents;

class ScreenShareServiceTest {

    static final long TEACHER = 1L, STUDENT = 2L, OTHER_STUDENT = 3L, SID = 10L;

    HelpRequestRepository helps = mock(HelpRequestRepository.class);
    StudySessionService sessions = mock(StudySessionService.class);
    UserService users = mock(UserService.class);
    LiveKitTokens tokens = mock(LiveKitTokens.class);
    SessionEvents events = mock(SessionEvents.class);
    ScreenShareService service = new ScreenShareService(helps, sessions, users, tokens, events, "ws://lk");

    HelpRequest help = new HelpRequest(SID, STUDENT, "막혔어요", true);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(help, "id", 5L);
        when(sessions.requireActiveRole(TEACHER, SID)).thenReturn(SessionRole.TEACHER);
        when(sessions.requireActiveRole(STUDENT, SID)).thenReturn(SessionRole.STUDENT);
        when(sessions.requireActiveRole(OTHER_STUDENT, SID)).thenReturn(SessionRole.STUDENT);
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "선생님만")).when(sessions).requireTeacherOf(eq(STUDENT), anyLong());
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "선생님만")).when(sessions).requireTeacherOf(eq(OTHER_STUDENT), anyLong());
        when(helps.findById(5L)).thenReturn(Optional.of(help));
        when(helps.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(users.namesOf(any())).thenReturn(Map.of());
        when(tokens.mint(any(), any(), any())).thenReturn("jwt");
    }

    private static HttpStatus status(Runnable call) {
        try {
            call.run();
        } catch (ApiException e) {
            return e.getStatus();
        }
        throw new AssertionError("ApiException 이 나야 함");
    }

    private LiveKitTokens.Grant grantFor(long userId) {
        ArgumentCaptor<LiveKitTokens.Grant> g = ArgumentCaptor.forClass(LiveKitTokens.Grant.class);
        verify(tokens).mint(eq("u" + userId), any(), g.capture());
        return g.getValue();
    }

    @Test
    void classRoomOnlyTeacherPublishes() {
        service.classToken(STUDENT, SID);
        LiveKitTokens.Grant g = grantFor(STUDENT);
        assertThat(g.room()).isEqualTo("session-10-class");
        assertThat(g.canPublishScreen()).isFalse();
        assertThat(g.canPublishData()).isFalse();

        service.classToken(TEACHER, SID);
        assertThat(grantFor(TEACHER).canPublishScreen()).isTrue();
    }

    @Test
    void onlyStudentsRequestAndOnlyOneOpenAtATime() {
        var req = new CreateHelpRequest("막혔어요", true);
        assertThat(status(() -> service.request(TEACHER, SID, req))).isEqualTo(HttpStatus.FORBIDDEN);
        when(helps.findBySessionIdAndUserIdAndStatusIn(eq(SID), eq(STUDENT), anyCollection())).thenReturn(List.of(help));
        assertThat(status(() -> service.request(STUDENT, SID, req))).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void acceptIsTeacherOnlyAndCappedAtFour() {
        assertThat(status(() -> service.accept(STUDENT, 5L))).isEqualTo(HttpStatus.FORBIDDEN);
        when(helps.countBySessionIdAndStatus(SID, Status.ACCEPTED)).thenReturn(4L);
        assertThat(status(() -> service.accept(TEACHER, 5L))).isEqualTo(HttpStatus.CONFLICT);
        when(helps.countBySessionIdAndStatus(SID, Status.ACCEPTED)).thenReturn(3L);
        service.accept(TEACHER, 5L);
        assertThat(help.getStatus()).isEqualTo(Status.ACCEPTED);
        verify(events).toUser(STUDENT, "helpAccepted", 5L);
    }

    @Test
    void helpRoomTokensFollowRoles() {
        assertThat(status(() -> service.helpToken(STUDENT, 5L))).isEqualTo(HttpStatus.CONFLICT);   // 아직 대기 중
        help.moveTo(Status.ACCEPTED);
        assertThat(status(() -> service.helpToken(OTHER_STUDENT, 5L))).isEqualTo(HttpStatus.FORBIDDEN);

        service.helpToken(STUDENT, 5L);
        LiveKitTokens.Grant s = grantFor(STUDENT);
        assertThat(s.room()).isEqualTo("session-10-help-5");
        assertThat(s.canPublishScreen()).isTrue();
        assertThat(s.canPublishData()).isFalse();   // 학생은 그림을 보낼 수 없음

        service.helpToken(TEACHER, 5L);
        LiveKitTokens.Grant t = grantFor(TEACHER);
        assertThat(t.canPublishScreen()).isFalse();
        assertThat(t.canPublishData()).isTrue();
    }

    @Test
    void endByOwnerOrTeacherButNotOthers() {
        assertThat(status(() -> service.end(OTHER_STUDENT, 5L))).isEqualTo(HttpStatus.FORBIDDEN);
        service.end(STUDENT, 5L);
        assertThat(help.getStatus()).isEqualTo(Status.DONE);
        assertThat(status(() -> service.end(TEACHER, 5L))).isEqualTo(HttpStatus.CONFLICT);   // 이미 끝남
    }
}
