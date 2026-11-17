package com.tutipal.domain.quiz.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tutipal.domain.quiz.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findBySessionIdOrderByIdAsc(Long sessionId);

    List<Quiz> findBySessionIdAndClosesAtAfterOrderByIdAsc(Long sessionId, Instant now);
}
