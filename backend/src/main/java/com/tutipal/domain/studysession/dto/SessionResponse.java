package com.tutipal.domain.studysession.dto;

import java.util.Locale;

import com.tutipal.domain.studysession.entity.SessionRole;
import com.tutipal.domain.studysession.entity.StudySession;

/** 세션 정보 + 내 역할. topic 은 화면 Topic 타입에 맞춰 소문자. */
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
        boolean ended) {

    public static SessionResponse of(StudySession s, SessionRole myRole, long participantCount) {
        return new SessionResponse(s.getId(), s.getTitle(), s.getTopic().name().toLowerCase(Locale.ROOT),
                s.getInviteCode(), myRole, s.getDifficulty(), s.isHideAnswers(), s.isPreferMaterials(),
                participantCount, s.isEnded());
    }
}
