package com.tutipal.domain.studysession.dto;

import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.entity.StudySession;

/** 세션 정보 + 내 역할. topic 은 과목 이름. */
public record SessionResponse(
        Long id,
        String title,
        String topic,
        String inviteCode,
        SessionRole myRole,
        int difficulty,
        boolean hideAnswers,
        boolean preferMaterials,
        long participantCount,
        boolean ended,
        /** 내가 즐겨찾기한 방 (홈 목록에서 맨 위) */
        boolean pinned) {

    public static SessionResponse of(StudySession s, SessionRole myRole, long participantCount) {
        return new SessionResponse(s.getId(), s.getTitle(), s.getTopic(),
                s.getInviteCode(), myRole, s.getDifficulty(), s.isHideAnswers(), s.isPreferMaterials(),
                participantCount, s.isEnded(), false);
    }

    public SessionResponse withPinned(boolean pinned) {
        return new SessionResponse(id, title, topic, inviteCode, myRole, difficulty, hideAnswers, preferMaterials,
                participantCount, ended, pinned);
    }
}
