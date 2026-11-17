package com.tutipal.domain.quiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tutipal.domain.quiz.entity.QuizAnswer;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    Optional<QuizAnswer> findByQuizIdAndUserId(Long quizId, Long userId);

    List<QuizAnswer> findByQuizIdOrderByIdAsc(Long quizId);

    List<QuizAnswer> findByQuizIdIn(List<Long> quizIds);
}
