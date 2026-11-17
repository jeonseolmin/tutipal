package com.tutipal.domain.studysession.service;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutipal.domain.studysession.dto.ChatSettings;
import com.tutipal.domain.studysession.dto.CreateSessionRequest;
import com.tutipal.domain.studysession.dto.ParticipantResponse;
import com.tutipal.domain.studysession.dto.SessionResponse;
import com.tutipal.domain.studysession.dto.UpdateSettingsRequest;
import com.tutipal.domain.studysession.entity.SessionParticipant;
import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.entity.StudySession;
import com.tutipal.domain.studysession.repository.SessionParticipantRepository;
import com.tutipal.domain.studysession.repository.StudySessionRepository;
import com.tutipal.domain.tutor.TopicPrompt;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.exception.ApiException;
import com.tutipal.global.realtime.Presence;
import com.tutipal.global.realtime.SessionEvents;

/**
 * 단체 학습 세션. 권한은 계정 역할이 아니라 "이 세션의 참가자/선생님인가" 로 여기서 확인한다.
 * 참가자가 아니면 세션이 있는지도 알려주지 않으려고 404 로 답한다.
 */
@Service
@Transactional(readOnly = true)
public class StudySessionService {

    private static final int CODE_ATTEMPTS = 10;

    private final StudySessionRepository sessions;
    private final SessionParticipantRepository participants;
    private final UserService userService;
    private final SessionEvents events;
    private final Presence presence;
    private final Clock clock = Clock.systemUTC();

    public StudySessionService(StudySessionRepository sessions, SessionParticipantRepository participants,
                               UserService userService, SessionEvents events, Presence presence) {
        this.sessions = sessions;
        this.participants = participants;
        this.userService = userService;
        this.events = events;
        this.presence = presence;
    }

    /** 세션을 연 사람이 그 세션의 TEACHER. */
    @Transactional
    public SessionResponse create(Long userId, CreateSessionRequest request) {
        String topic = TopicPrompt.subject(request.topic());
        StudySession session = sessions.save(new StudySession(request.title().trim(), topic, newCode(), userId));
        participants.save(new SessionParticipant(session.getId(), userId, SessionRole.TEACHER));
        return SessionResponse.of(session, SessionRole.TEACHER, 1);
    }

    /**
     * 초대 코드로 참여 → STUDENT. 이미 참가자면 원래 역할 그대로 (선생님이 코드를 넣어도 선생님).
     * ponytail: 코드 맞히기 시도 제한 없음 (31^6 ≈ 8.9억 가지), 공개 운영 전에 사용자별 시도 제한 추가
     */
    @Transactional
    public SessionResponse join(Long userId, String code) {
        StudySession session = sessions.findByInviteCode(InviteCodes.normalize(code))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "코드에 맞는 세션이 없어요"));
        if (session.isEnded()) {
            throw new ApiException(HttpStatus.GONE, "이미 끝난 세션이에요");
        }
        SessionParticipant me = participants.findBySessionIdAndUserId(session.getId(), userId)
                .orElseGet(() -> {
                    events.toTeacher(session.getId(), "joined", userId);
                    return participants.save(new SessionParticipant(session.getId(), userId, SessionRole.STUDENT));
                });
        return response(session, me.getRole());
    }

    /** 내가 참가 중인(끝나지 않은) 세션, 최근 것부터. */
    public List<SessionResponse> mySessions(Long userId) {
        Map<Long, SessionRole> roles = participants.findByUserId(userId).stream()
                .collect(Collectors.toMap(SessionParticipant::getSessionId, SessionParticipant::getRole));
        return sessions.findAllById(roles.keySet()).stream()
                .filter(s -> !s.isEnded())
                .sorted(Comparator.comparing(StudySession::getId).reversed())
                .map(s -> response(s, roles.get(s.getId())))
                .toList();
    }

    public SessionResponse get(Long userId, Long sessionId) {
        SessionParticipant me = requireParticipant(sessionId, userId);
        return response(findSession(sessionId), me.getRole());
    }

    /** 목업 20: 선생님만. */
    public List<ParticipantResponse> participants(Long userId, Long sessionId) {
        requireTeacher(sessionId, userId);
        List<SessionParticipant> list = participants.findBySessionIdOrderByIdAsc(sessionId);
        Map<Long, String> names = userService.namesOf(list.stream().map(SessionParticipant::getUserId).toList());
        Set<Long> online = presence.onlineUsers(sessionId);
        return list.stream()
                .map(p -> new ParticipantResponse(p.getUserId(), names.getOrDefault(p.getUserId(), "(알 수 없음)"),
                        p.getRole(), p.getCreatedAt(), online.contains(p.getUserId())))
                .toList();
    }

    /** 목업 21: 선생님 설정 → 학생 챗봇에 다음 질문부터 적용. */
    @Transactional
    public SessionResponse updateSettings(Long userId, Long sessionId, UpdateSettingsRequest request) {
        requireTeacher(sessionId, userId);
        StudySession session = requireActive(sessionId);
        session.updateSettings(TopicPrompt.subject(request.topic()), request.difficulty(),
                request.hideAnswers(), request.preferMaterials());
        events.toSession(sessionId, "settings", sessionId);
        return response(session, SessionRole.TEACHER);
    }

    /** 세션 종료: 새 참여 · 세션 챗봇이 막힌다. 여러 번 불러도 같음. */
    @Transactional
    public SessionResponse end(Long userId, Long sessionId) {
        requireTeacher(sessionId, userId);
        StudySession session = findSession(sessionId);
        if (!session.isEnded()) {
            session.end(clock.instant());
            events.toSession(sessionId, "ended", sessionId);
        }
        return response(session, SessionRole.TEACHER);
    }

    /** tutor 도메인용: 참가자이고 진행 중인 세션이면 선생님 챗봇 설정. */
    public ChatSettings chatSettings(Long userId, Long sessionId) {
        requireParticipant(sessionId, userId);
        StudySession s = requireActive(sessionId);
        return new ChatSettings(s.getTopic(), s.getDifficulty(), s.isHideAnswers(), s.isPreferMaterials());
    }

    /** STOMP 구독 검사용: 진행 중인 세션의 참가자면 역할, 아니면 empty. */
    public Optional<SessionRole> roleOf(Long userId, Long sessionId) {
        return participants.findBySessionIdAndUserId(sessionId, userId)
                .filter(p -> sessions.findById(sessionId).filter(s -> !s.isEnded()).isPresent())
                .map(SessionParticipant::getRole);
    }

    /** 다른 도메인(quiz)용: 진행 중인 세션의 참가자인지 확인하고 역할을 돌려준다 (아니면 404 · 410). */
    public SessionRole requireActiveRole(Long userId, Long sessionId) {
        SessionRole role = requireParticipant(sessionId, userId).getRole();
        requireActive(sessionId);
        return role;
    }

    /** 다른 도메인(quiz)용: 그 세션의 선생님인지 확인 (아니면 404 · 403). */
    public void requireTeacherOf(Long userId, Long sessionId) {
        requireTeacher(sessionId, userId);
    }

    /** 이해도 질문 응답률 분모: 학생 수. */
    public long studentCount(Long sessionId) {
        return participants.countBySessionIdAndRole(sessionId, SessionRole.STUDENT);
    }

    private SessionParticipant requireParticipant(Long sessionId, Long userId) {
        return participants.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없어요"));
    }

    private void requireTeacher(Long sessionId, Long userId) {
        if (!requireParticipant(sessionId, userId).isTeacher()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "이 세션의 선생님만 할 수 있어요");
        }
    }

    private StudySession findSession(Long sessionId) {
        return sessions.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없어요"));
    }

    private StudySession requireActive(Long sessionId) {
        StudySession session = findSession(sessionId);
        if (session.isEnded()) {
            throw new ApiException(HttpStatus.GONE, "이미 끝난 세션이에요");
        }
        return session;
    }

    private SessionResponse response(StudySession session, SessionRole myRole) {
        return SessionResponse.of(session, myRole, participants.countBySessionId(session.getId()));
    }

    private String newCode() {
        for (int i = 0; i < CODE_ATTEMPTS; i++) {
            String code = InviteCodes.generate();
            if (!sessions.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "초대 코드를 만들지 못했어요. 다시 시도해 주세요");
    }
}
