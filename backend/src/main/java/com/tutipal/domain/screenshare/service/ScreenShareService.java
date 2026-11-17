package com.tutipal.domain.screenshare.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutipal.domain.screenshare.dto.ScreenShareDtos.CreateHelpRequest;
import com.tutipal.domain.screenshare.dto.ScreenShareDtos.HelpResponse;
import com.tutipal.domain.screenshare.dto.ScreenShareDtos.JoinInfo;
import com.tutipal.domain.screenshare.entity.HelpRequest;
import com.tutipal.domain.screenshare.entity.HelpRequest.Status;
import com.tutipal.domain.screenshare.repository.HelpRequestRepository;
import com.tutipal.domain.screenshare.service.LiveKitTokens.Grant;
import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.service.StudySessionService;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.exception.ApiException;
import com.tutipal.global.realtime.SessionEvents;

/**
 * 화면공유 (LiveKit).
 * <ul>
 *   <li>방 A session-{id}-class: 선생님 화면 → 학생 전체 (선생님만 화면 트랙을 올릴 수 있음)</li>
 *   <li>방 B session-{id}-help-{요청id}: 수락된 도움 요청 1:1. 학생은 화면만 올리고, 선생님은 보기 + 그림(데이터)만</li>
 * </ul>
 * 권한은 여기서 확인하고, LiveKit 에는 그 결과만 입장권으로 준다.
 * ponytail: 끝난 요청의 방에서 강제로 내보내지는 않음 (입장권 10분 + 화면이 종료 알림에 나감). 필요하면 LiveKit RoomService API 로 강제 퇴장
 */
@Service
@Transactional(readOnly = true)
public class ScreenShareService {

    /** 결정: 학생 → 선생님 1:1 은 동시에 최대 4명 */
    static final int MAX_ACTIVE_HELP = 4;
    private static final Set<Status> OPEN = Set.of(Status.PENDING, Status.ACCEPTED);

    private final HelpRequestRepository helps;
    private final StudySessionService sessions;
    private final UserService users;
    private final LiveKitTokens tokens;
    private final SessionEvents events;
    private final String publicUrl;

    public ScreenShareService(HelpRequestRepository helps, StudySessionService sessions, UserService users,
                              LiveKitTokens tokens, SessionEvents events,
                              @Value("${app.livekit.public-url}") String publicUrl) {
        this.helps = helps;
        this.sessions = sessions;
        this.users = users;
        this.tokens = tokens;
        this.events = events;
        this.publicUrl = publicUrl;
    }

    /** 방 A 입장권: 선생님은 화면 공유 + 보기, 학생은 보기만. */
    public JoinInfo classToken(Long userId, Long sessionId) {
        boolean teacher = sessions.requireActiveRole(userId, sessionId) == SessionRole.TEACHER;
        return join(userId, new Grant("session-" + sessionId + "-class", teacher, true, false));
    }

    /** 목업 17: 학생만, 열린 요청은 하나만. */
    @Transactional
    public HelpResponse request(Long userId, Long sessionId, CreateHelpRequest req) {
        if (sessions.requireActiveRole(userId, sessionId) != SessionRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "학생만 도움을 요청할 수 있어요");
        }
        if (!helps.findBySessionIdAndUserIdAndStatusIn(sessionId, userId, OPEN).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 보낸 도움 요청이 있어요");
        }
        HelpRequest help = helps.save(new HelpRequest(sessionId, userId, req.message().strip(), req.shareScreen()));
        events.toTeacher(sessionId, "help", help.getId());
        return responses(List.of(help)).getFirst();
    }

    /** 목업 25: 선생님은 대기 · 진행 중 전체, 학생은 내 열린 요청만. */
    public List<HelpResponse> list(Long userId, Long sessionId) {
        boolean teacher = sessions.requireActiveRole(userId, sessionId) == SessionRole.TEACHER;
        return responses(teacher
                ? helps.findBySessionIdAndStatusInOrderByIdAsc(sessionId, OPEN)
                : helps.findBySessionIdAndUserIdAndStatusIn(sessionId, userId, OPEN));
    }

    @Transactional
    public HelpResponse accept(Long userId, Long helpId) {
        HelpRequest help = findOpen(helpId);
        sessions.requireTeacherOf(userId, help.getSessionId());
        if (help.getStatus() != Status.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 처리된 요청이에요");
        }
        if (helps.countBySessionIdAndStatus(help.getSessionId(), Status.ACCEPTED) >= MAX_ACTIVE_HELP) {
            throw new ApiException(HttpStatus.CONFLICT, "동시에 " + MAX_ACTIVE_HELP + "명까지 도와줄 수 있어요. 진행 중인 도움을 먼저 끝내 주세요");
        }
        help.moveTo(Status.ACCEPTED);
        notifyBoth(help, "helpAccepted");
        return responses(List.of(help)).getFirst();
    }

    /** "나중에": 학생에게 알리고 닫는다 (학생이 다시 요청할 수 있음). */
    @Transactional
    public void later(Long userId, Long helpId) {
        HelpRequest help = findOpen(helpId);
        sessions.requireTeacherOf(userId, help.getSessionId());
        if (help.getStatus() != Status.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "진행 중인 도움은 '종료' 로 끝내 주세요");
        }
        help.moveTo(Status.DECLINED);
        notifyBoth(help, "helpDeclined");
    }

    /** 끝내기: 그 세션 선생님 또는 요청한 학생. */
    @Transactional
    public void end(Long userId, Long helpId) {
        HelpRequest help = findOpen(helpId);
        if (!help.getUserId().equals(userId)) {
            sessions.requireTeacherOf(userId, help.getSessionId());
        }
        help.moveTo(Status.DONE);
        notifyBoth(help, "helpEnded");
    }

    /** 방 B 입장권: 진행 중(ACCEPTED)일 때 요청한 학생과 그 세션 선생님만. */
    public JoinInfo helpToken(Long userId, Long helpId) {
        HelpRequest help = helps.findById(helpId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "도움 요청을 찾을 수 없어요"));
        boolean student = help.getUserId().equals(userId);
        if (!student) {
            sessions.requireTeacherOf(userId, help.getSessionId());   // 아니면 404 · 403
        }
        sessions.requireActiveRole(userId, help.getSessionId());       // 끝난 세션이면 410
        if (help.getStatus() != Status.ACCEPTED) {
            throw new ApiException(HttpStatus.CONFLICT, "진행 중인 도움이 아니에요");
        }
        String room = "session-" + help.getSessionId() + "-help-" + help.getId();
        return join(userId, student
                ? new Grant(room, help.isShareScreen(), true, false)   // 학생: 내 화면만 올림, 그림은 받기만
                : new Grant(room, false, true, true));                 // 선생님: 보기 + 그림 보내기
    }

    private JoinInfo join(Long userId, Grant grant) {
        String identity = "u" + userId;
        String name = users.namesOf(List.of(userId)).getOrDefault(userId, identity);
        return new JoinInfo(publicUrl, tokens.mint(identity, name, grant), grant.room(), identity);
    }

    private void notifyBoth(HelpRequest help, String studentEvent) {
        events.toUser(help.getUserId(), studentEvent, help.getId());
        events.toTeacher(help.getSessionId(), "help", help.getId());
    }

    private HelpRequest findOpen(Long helpId) {
        HelpRequest help = helps.findById(helpId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "도움 요청을 찾을 수 없어요"));
        if (!help.isOpen()) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 끝난 요청이에요");
        }
        return help;
    }

    private List<HelpResponse> responses(List<HelpRequest> list) {
        Map<Long, String> names = users.namesOf(list.stream().map(HelpRequest::getUserId).toList());
        return list.stream()
                .map(h -> new HelpResponse(h.getId(), h.getUserId(), names.getOrDefault(h.getUserId(), "(알 수 없음)"),
                        h.getMessage(), h.isShareScreen(), h.getStatus(), h.getCreatedAt()))
                .toList();
    }
}
