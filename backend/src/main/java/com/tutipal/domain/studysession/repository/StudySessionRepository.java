package com.tutipal.domain.studysession.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tutipal.domain.studysession.entity.StudySession;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    Optional<StudySession> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
