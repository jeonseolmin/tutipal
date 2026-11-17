package com.tutipal.domain.quiz.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutipal.domain.quiz.dto.QuizDtos.AnswerRequest;
import com.tutipal.domain.quiz.dto.QuizDtos.AnswerRow;
import com.tutipal.domain.quiz.dto.QuizDtos.CreateQuizRequest;
import com.tutipal.domain.quiz.dto.QuizDtos.GenerateQuizRequest;
import com.tutipal.domain.quiz.dto.QuizDtos.MyAnswer;
import com.tutipal.domain.quiz.dto.QuizDtos.QuizDraft;
import com.tutipal.domain.quiz.dto.QuizDtos.QuizSummary;
import com.tutipal.domain.quiz.dto.QuizDtos.StudentQuiz;
import com.tutipal.domain.quiz.entity.GradedBy;
import com.tutipal.domain.quiz.entity.Quiz;
import com.tutipal.domain.quiz.entity.QuizAnswer;
import com.tutipal.domain.quiz.entity.QuizType;
import com.tutipal.domain.quiz.repository.QuizAnswerRepository;
import com.tutipal.domain.quiz.repository.QuizRepository;
import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.service.StudySessionService;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.common.AfterCommit;
import com.tutipal.global.exception.ApiException;
import com.tutipal.global.realtime.SessionEvents;

/**
 * 이해도 확인 질문. 권한: 만들기 · 닫기 · 결과 보기 · 채점 수정은 그 세션 선생님, 응답은 그 세션 학생만.
 * 객관식은 응답 즉시 채점, 주관식은 커밋 뒤 가상 스레드에서 AI 채점 → 학생에게 결과 알림.
 */
@Service
@Transactional(readOnly = true)
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    /** 마감 직전에 누른 제출이 네트워크 지연으로 늦게 와도 받아 준다. */
    private static final Duration GRACE = Duration.ofSeconds(3);
    private static final int DEFAULT_TIME_LIMIT = 60;
    static final String AI_FAILED = "AI 채점을 하지 못했어요. 선생님이 확인해 줄 거예요";

    private final QuizRepository quizzes;
    private final QuizAnswerRepository answers;
    private final StudySessionService sessions;
    private final UserService users;
    private final QuizAi quizAi;
    private final SessionEvents events;
    private final Clock clock;

    @Autowired
    public QuizService(QuizRepository quizzes, QuizAnswerRepository answers, StudySessionService sessions,
                       UserService users, QuizAi quizAi, SessionEvents events) {
        this(quizzes, answers, sessions, users, quizAi, events, Clock.systemUTC());
    }

    QuizService(QuizRepository quizzes, QuizAnswerRepository answers, StudySessionService sessions,
                UserService users, QuizAi quizAi, SessionEvents events, Clock clock) {
        this.quizzes = quizzes;
        this.answers = answers;
        this.sessions = sessions;
        this.users = users;
        this.quizAi = quizAi;
        this.events = events;
        this.clock = clock;
    }

    /** 목업 22: 질문을 만들어 바로 학생 전체에게 보낸다. */
    @Transactional
    public QuizSummary create(Long userId, Long sessionId, CreateQuizRequest req) {
        sessions.requireTeacherOf(userId, sessionId);
        sessions.requireActiveRole(userId, sessionId);   // 끝난 세션이면 410
        List<String> choices = null;
        Integer answerIndex = null;
        String modelAnswer = null;
        if (req.type() == QuizType.CHOICE) {
            choices = req.choices() == null ? List.of() : req.choices().stream().map(String::strip).toList();
            if (choices.size() < 2) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "보기는 2개 이상 필요해요");
            }
            if (req.answerIndex() == null || req.answerIndex() < 0 || req.answerIndex() >= choices.size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "정답 보기를 골라 주세요");
            }
            answerIndex = req.answerIndex();
        } else {
            if (req.modelAnswer() == null || req.modelAnswer().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "AI 채점 기준이 되는 모범 답안이 필요해요");
            }
            modelAnswer = req.modelAnswer().strip();
        }
        int limit = req.timeLimitSec() == null ? DEFAULT_TIME_LIMIT : req.timeLimitSec();
        Quiz quiz = quizzes.save(new Quiz(sessionId, req.type(), req.question().strip(), choices, answerIndex,
                modelAnswer, clock.instant().plusSeconds(limit)));
        events.toSession(sessionId, "quiz", quiz.getId());
        return summaries(sessionId).getLast();
    }

    /** 목업 22 "AI로 문제 만들기": 저장하지 않고 초안만 돌려준다. */
    public QuizDraft generate(Long userId, Long sessionId, GenerateQuizRequest req) {
        sessions.requireTeacherOf(userId, sessionId);
        return quizAi.generate(sessions.chatSettings(userId, sessionId), req.type(), req.hint());
    }

    /** 목업 23: 질문별 정답률 (선생님). */
    public List<QuizSummary> list(Long userId, Long sessionId) {
        sessions.requireTeacherOf(userId, sessionId);
        return summaries(sessionId);
    }

    /** 학생이 세션 화면을 열 때: 아직 열려 있는 질문 (보통 0~1개). */
    public List<StudentQuiz> openForStudent(Long userId, Long sessionId) {
        sessions.requireActiveRole(userId, sessionId);
        return quizzes.findBySessionIdAndClosesAtAfterOrderByIdAsc(sessionId, clock.instant()).stream()
                .map(q -> studentView(q, userId))
                .toList();
    }

    public StudentQuiz getForStudent(Long userId, Long quizId) {
        Quiz quiz = findQuiz(quizId);
        sessions.requireActiveRole(userId, quiz.getSessionId());
        return studentView(quiz, userId);
    }

    /** 목업 15 → 16: 학생 응답. 한 질문에 한 번만. */
    @Transactional
    public MyAnswer answer(Long userId, Long quizId, AnswerRequest req) {
        Quiz quiz = findQuiz(quizId);
        if (sessions.requireActiveRole(userId, quiz.getSessionId()) != SessionRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "학생만 응답할 수 있어요");
        }
        if (!quiz.isOpen(clock.instant().minus(GRACE))) {
            throw new ApiException(HttpStatus.GONE, "응답 시간이 끝났어요");
        }
        if (answers.findByQuizIdAndUserId(quizId, userId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 응답했어요");
        }
        QuizAnswer answer;
        if (quiz.getType() == QuizType.CHOICE) {
            Integer idx = req.choiceIndex();
            if (idx == null || idx < 0 || idx >= quiz.getChoices().size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "보기를 골라 주세요");
            }
            answer = new QuizAnswer(quizId, userId, idx, null);
            answer.grade(idx.equals(quiz.getAnswerIndex()), GradedBy.AUTO, null);
            answers.save(answer);
        } else {
            if (req.text() == null || req.text().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "답을 입력해 주세요");
            }
            answer = answers.save(new QuizAnswer(quizId, userId, null, req.text().strip()));
            Long answerId = answer.getId();
            AfterCommit.run(() -> Thread.ofVirtual().name("quiz-grade").start(() -> gradeWithAi(answerId)));
        }
        events.toTeacher(quiz.getSessionId(), "quizStats", quizId);
        return mine(answer);
    }

    /** 일찍 닫기 (선생님). */
    @Transactional
    public QuizSummary close(Long userId, Long quizId) {
        Quiz quiz = findQuiz(quizId);
        sessions.requireTeacherOf(userId, quiz.getSessionId());
        quiz.close(clock.instant());
        events.toSession(quiz.getSessionId(), "quizClosed", quizId);
        return summaries(quiz.getSessionId()).stream().filter(s -> s.id().equals(quizId)).findFirst().orElseThrow();
    }

    /** 목업 24: 응답 목록 (선생님). */
    public List<AnswerRow> answers(Long userId, Long quizId) {
        Quiz quiz = findQuiz(quizId);
        sessions.requireTeacherOf(userId, quiz.getSessionId());
        List<QuizAnswer> list = answers.findByQuizIdOrderByIdAsc(quizId);
        Map<Long, String> names = users.namesOf(list.stream().map(QuizAnswer::getUserId).toList());
        return list.stream()
                .map(a -> new AnswerRow(a.getId(), a.getUserId(), names.getOrDefault(a.getUserId(), "(알 수 없음)"),
                        a.getChoiceIndex(), a.getText(), a.getCorrect(), a.getGradedBy(), a.getFeedback()))
                .toList();
    }

    /** 목업 24 "맞음/틀림으로 변경": 선생님 판정이 최종. */
    @Transactional
    public AnswerRow regrade(Long userId, Long answerId, boolean correct) {
        QuizAnswer answer = answers.findById(answerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "응답을 찾을 수 없어요"));
        Quiz quiz = findQuiz(answer.getQuizId());
        sessions.requireTeacherOf(userId, quiz.getSessionId());
        // AI 피드백은 판정이 그대로일 때만 남김 (정답으로 바꿨는데 "다시 생각해 보세요" 가 남으면 헷갈림).
        // 채점 전(AI 실패 안내문)이었으면 지움.
        String feedback = !answer.isPending() && Boolean.valueOf(correct).equals(answer.getCorrect())
                ? answer.getFeedback() : null;
        answer.grade(correct, GradedBy.TEACHER, feedback);
        events.toUser(answer.getUserId(), "quizResult", quiz.getId());
        events.toTeacher(quiz.getSessionId(), "quizStats", quiz.getId());
        return answers(userId, quiz.getId()).stream().filter(r -> r.answerId().equals(answerId)).findFirst().orElseThrow();
    }

    /** 커밋 뒤 가상 스레드에서. AI 호출 중에는 트랜잭션을 잡지 않는다 (저장은 save 한 번). */
    void gradeWithAi(Long answerId) {
        QuizAnswer answer = answers.findById(answerId).orElse(null);
        if (answer == null || !answer.isPending()) {
            return;
        }
        Quiz quiz = findQuiz(answer.getQuizId());
        QuizAi.Grade grade = null;
        try {
            grade = quizAi.grade(quiz.getQuestion(), quiz.getModelAnswer(), answer.getText());
        } catch (RuntimeException e) {
            log.warn("quiz AI grading failed: answer={} {}", answerId, e.getClass().getSimpleName());
        }
        // ponytail: 다시 읽고 저장 사이의 아주 짧은 틈에 선생님 채점이 겹치면 덮어쓸 수 있음 (@Version 으로 막을 수 있음)
        QuizAnswer fresh = answers.findById(answerId).orElse(null);
        if (fresh == null || !fresh.isPending()) {
            return;   // 그사이 선생님이 먼저 채점함
        }
        if (grade != null) {
            fresh.grade(grade.correct(), GradedBy.AI, grade.feedback());
        } else {
            fresh.gradingFailed(AI_FAILED);
        }
        answers.save(fresh);
        events.toUser(answer.getUserId(), "quizResult", quiz.getId());
        events.toTeacher(quiz.getSessionId(), "quizStats", quiz.getId());
    }

    private List<QuizSummary> summaries(Long sessionId) {
        List<Quiz> list = quizzes.findBySessionIdOrderByIdAsc(sessionId);
        Map<Long, List<QuizAnswer>> byQuiz = answers.findByQuizIdIn(list.stream().map(Quiz::getId).toList())
                .stream().collect(Collectors.groupingBy(QuizAnswer::getQuizId));
        long students = sessions.studentCount(sessionId);
        Instant now = clock.instant();
        List<QuizSummary> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Quiz q = list.get(i);
            List<QuizAnswer> as = byQuiz.getOrDefault(q.getId(), List.of());
            out.add(new QuizSummary(q.getId(), i + 1, q.getType(), q.getQuestion(), q.getChoices(),
                    q.getAnswerIndex(), q.getModelAnswer(), q.getClosesAt(), q.isOpen(now),
                    as.size(),
                    as.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count(),
                    as.stream().filter(QuizAnswer::isPending).count(),
                    students));
        }
        return out;
    }

    private StudentQuiz studentView(Quiz q, Long userId) {
        MyAnswer my = answers.findByQuizIdAndUserId(q.getId(), userId).map(QuizService::mine).orElse(null);
        return new StudentQuiz(q.getId(), q.getSessionId(), q.getType(), q.getQuestion(), q.getChoices(),
                q.getClosesAt(), q.isOpen(clock.instant()), my);
    }

    private static MyAnswer mine(QuizAnswer a) {
        return new MyAnswer(a.getChoiceIndex(), a.getText(), a.getCorrect(), a.getGradedBy(), a.getFeedback());
    }

    private Quiz findQuiz(Long quizId) {
        return quizzes.findById(quizId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "질문을 찾을 수 없어요"));
    }
}
