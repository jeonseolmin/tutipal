package com.tutipal.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.tutipal.domain.quiz.dto.QuizDtos.AnswerRequest;
import com.tutipal.domain.quiz.dto.QuizDtos.CreateQuizRequest;
import com.tutipal.domain.quiz.entity.GradedBy;
import com.tutipal.domain.quiz.entity.Quiz;
import com.tutipal.domain.quiz.entity.QuizAnswer;
import com.tutipal.domain.quiz.entity.QuizType;
import com.tutipal.domain.quiz.repository.QuizAnswerRepository;
import com.tutipal.domain.quiz.repository.QuizRepository;
import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.service.StudySessionService;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.exception.ApiException;
import com.tutipal.global.realtime.SessionEvents;

class QuizServiceTest {

    static final long TEACHER = 1L, STUDENT = 2L, SID = 10L;
    static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");

    QuizRepository quizzes = mock(QuizRepository.class);
    QuizAnswerRepository answers = mock(QuizAnswerRepository.class);
    StudySessionService sessions = mock(StudySessionService.class);
    QuizAi quizAi = mock(QuizAi.class);
    SessionEvents events = mock(SessionEvents.class);
    QuizService service = new QuizService(quizzes, answers, sessions, mock(UserService.class), quizAi, events,
            Clock.fixed(NOW, ZoneOffset.UTC));

    Quiz choice = quiz(100L, QuizType.CHOICE, NOW.plusSeconds(30));
    Quiz text = quiz(200L, QuizType.TEXT, NOW.plusSeconds(30));

    static Quiz quiz(long id, QuizType type, Instant closesAt) {
        Quiz q = type == QuizType.CHOICE
                ? new Quiz(SID, type, "2+2?", List.of("3", "4", "5"), 1, null, closesAt)
                : new Quiz(SID, type, "변수란?", null, null, "값을 담는 이름", closesAt);
        ReflectionTestUtils.setField(q, "id", id);
        return q;
    }

    @BeforeEach
    void setUp() {
        when(quizzes.findById(100L)).thenReturn(Optional.of(choice));
        when(quizzes.findById(200L)).thenReturn(Optional.of(text));
        when(sessions.requireActiveRole(TEACHER, SID)).thenReturn(SessionRole.TEACHER);
        when(sessions.requireActiveRole(STUDENT, SID)).thenReturn(SessionRole.STUDENT);
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "선생님만")).when(sessions).requireTeacherOf(STUDENT, SID);
        when(answers.findByQuizIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(answers.save(any())).thenAnswer(inv -> {
            QuizAnswer a = inv.getArgument(0);
            if (a.getId() == null) {
                ReflectionTestUtils.setField(a, "id", 7L);
            }
            return a;
        });
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
    void choiceAnswerIsGradedImmediately() {
        var mine = service.answer(STUDENT, 100L, new AnswerRequest(1, null));
        assertThat(mine.correct()).isTrue();
        assertThat(mine.gradedBy()).isEqualTo(GradedBy.AUTO);
        assertThat(service.answer(STUDENT, 100L, new AnswerRequest(0, null)).correct()).isFalse();
        verify(events, times(2)).toTeacher(SID, "quizStats", 100L);
    }

    @Test
    void rejectsTeacherDuplicateLateAndBadChoice() {
        assertThat(status(() -> service.answer(TEACHER, 100L, new AnswerRequest(1, null)))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(() -> service.answer(STUDENT, 100L, new AnswerRequest(9, null)))).isEqualTo(HttpStatus.BAD_REQUEST);

        when(answers.findByQuizIdAndUserId(100L, STUDENT)).thenReturn(Optional.of(new QuizAnswer(100L, STUDENT, 1, null)));
        assertThat(status(() -> service.answer(STUDENT, 100L, new AnswerRequest(1, null)))).isEqualTo(HttpStatus.CONFLICT);

        choice.close(NOW.minusSeconds(10));   // 10초 전에 마감 → 3초 유예도 지남
        ReflectionTestUtils.setField(choice, "closesAt", NOW.minusSeconds(10));
        assertThat(status(() -> service.answer(STUDENT, 100L, new AnswerRequest(1, null)))).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void lateWithinGraceIsAccepted() {
        ReflectionTestUtils.setField(choice, "closesAt", NOW.minusSeconds(2));
        assertThat(service.answer(STUDENT, 100L, new AnswerRequest(1, null)).correct()).isTrue();
    }

    @Test
    void textAnswerStartsPending() {
        // 트랜잭션 밖이라 채점 스레드가 바로 뜨지만 findById 가 비어 있어 아무것도 안 함
        var mine = service.answer(STUDENT, 200L, new AnswerRequest(null, "  값을 저장하는 공간  "));
        assertThat(mine.gradedBy()).isNull();   // 채점 중
        assertThat(mine.text()).isEqualTo("값을 저장하는 공간");
        assertThat(status(() -> service.answer(STUDENT, 200L, new AnswerRequest(null, " ")))).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aiGradeIsSavedAndStudentNotified() {
        QuizAnswer saved = new QuizAnswer(200L, STUDENT, null, "값을 저장하는 공간");
        when(answers.findById(7L)).thenReturn(Optional.of(saved));
        when(quizAi.grade(anyString(), anyString(), anyString())).thenReturn(new QuizAi.Grade(true, "잘했어요"));
        service.gradeWithAi(7L);
        assertThat(saved.getCorrect()).isTrue();
        assertThat(saved.getGradedBy()).isEqualTo(GradedBy.AI);
        verify(events).toUser(STUDENT, "quizResult", 200L);
    }

    @Test
    void aiFailureLeavesPendingWithNote() {
        QuizAnswer saved = new QuizAnswer(200L, STUDENT, null, "몰라요");
        when(answers.findById(7L)).thenReturn(Optional.of(saved));
        when(quizAi.grade(anyString(), anyString(), anyString())).thenThrow(new IllegalArgumentException("이상한 JSON"));
        service.gradeWithAi(7L);
        assertThat(saved.isPending()).isTrue();
        assertThat(saved.getFeedback()).isEqualTo(QuizService.AI_FAILED);
    }

    @Test
    void teacherGradeIsNotOverwrittenByLateAi() {
        QuizAnswer saved = new QuizAnswer(200L, STUDENT, null, "몰라요");
        saved.grade(true, GradedBy.TEACHER, null);
        when(answers.findById(7L)).thenReturn(Optional.of(saved));
        service.gradeWithAi(7L);
        assertThat(saved.getGradedBy()).isEqualTo(GradedBy.TEACHER);
    }

    @Test
    void teacherFlipDropsContradictingAiFeedback() {
        QuizAnswer a = new QuizAnswer(200L, STUDENT, null, "명령어");
        a.grade(false, GradedBy.AI, "다시 생각해 보세요");
        ReflectionTestUtils.setField(a, "id", 7L);
        when(answers.findById(7L)).thenReturn(Optional.of(a));
        when(answers.findByQuizIdOrderByIdAsc(200L)).thenReturn(List.of(a));
        service.regrade(TEACHER, 7L, true);
        assertThat(a.getCorrect()).isTrue();
        assertThat(a.getGradedBy()).isEqualTo(GradedBy.TEACHER);
        assertThat(a.getFeedback()).isNull();
        verify(events).toUser(STUDENT, "quizResult", 200L);
    }

    @Test
    void onlyTeacherCreatesAndChoiceNeedsValidAnswer() {
        var req = new CreateQuizRequest(QuizType.CHOICE, "2+2?", List.of("3", "4"), 5, null, 30);
        assertThat(status(() -> service.create(STUDENT, SID, req))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(status(() -> service.create(TEACHER, SID, req))).isEqualTo(HttpStatus.BAD_REQUEST);
        var noModel = new CreateQuizRequest(QuizType.TEXT, "변수란?", null, null, " ", 30);
        assertThat(status(() -> service.create(TEACHER, SID, noModel))).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
