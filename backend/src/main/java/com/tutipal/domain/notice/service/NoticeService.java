package com.tutipal.domain.notice.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutipal.domain.notice.dto.NoticeDtos.CreateNotice;
import com.tutipal.domain.notice.dto.NoticeDtos.NoticeResponse;
import com.tutipal.domain.notice.entity.Notice;
import com.tutipal.domain.notice.repository.NoticeRepository;
import com.tutipal.domain.studysession.service.StudySessionService;
import com.tutipal.domain.user.service.UserService;
import com.tutipal.global.exception.ApiException;
import com.tutipal.global.realtime.SessionEvents;

/** 공지: 선생님만 쓰고 지움, 참가자 모두 읽음. 알림을 켜면 notice, 아니면 notices(목록만 새로) 이벤트. */
@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository notices;
    private final StudySessionService sessions;
    private final UserService users;
    private final SessionEvents events;

    public NoticeService(NoticeRepository notices, StudySessionService sessions, UserService users, SessionEvents events) {
        this.notices = notices;
        this.sessions = sessions;
        this.users = users;
        this.events = events;
    }

    @Transactional
    public NoticeResponse create(Long userId, Long sessionId, CreateNotice req) {
        sessions.requireTeacherOf(userId, sessionId);
        sessions.requireActiveRole(userId, sessionId);
        Notice n = notices.save(new Notice(sessionId, userId, req.title().strip(), req.body().strip()));
        events.toSession(sessionId, req.alert() ? "notice" : "notices", n.getId());
        return responses(List.of(n)).getFirst();
    }

    public List<NoticeResponse> list(Long userId, Long sessionId) {
        sessions.requireActiveRole(userId, sessionId);
        return responses(notices.findBySessionIdOrderByIdDesc(sessionId));
    }

    @Transactional
    public void delete(Long userId, Long noticeId) {
        Notice n = notices.findById(noticeId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "공지를 찾을 수 없어요"));
        sessions.requireTeacherOf(userId, n.getSessionId());
        notices.delete(n);
        events.toSession(n.getSessionId(), "notices", noticeId);
    }

    private List<NoticeResponse> responses(List<Notice> list) {
        Map<Long, String> names = users.namesOf(list.stream().map(Notice::getAuthorId).toList());
        return list.stream()
                .map(n -> new NoticeResponse(n.getId(), n.getTitle(), n.getBody(), names.getOrDefault(n.getAuthorId(), "선생님"), n.getCreatedAt()))
                .toList();
    }
}
