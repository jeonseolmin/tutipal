package com.tutipal.domain.studysession.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tutipal.domain.studysession.entity.SessionParticipant;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Long> {

    Optional<SessionParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);

    List<SessionParticipant> findBySessionIdOrderByIdAsc(Long sessionId);

    List<SessionParticipant> findByUserId(Long userId);

    long countBySessionId(Long sessionId);

    long countBySessionIdAndRole(Long sessionId, com.tutipal.domain.studysession.entity.SessionRole role);
}
