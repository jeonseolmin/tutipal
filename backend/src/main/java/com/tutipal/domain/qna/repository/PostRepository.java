package com.tutipal.domain.qna.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tutipal.domain.qna.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findBySessionIdOrderByIdDesc(Long sessionId);
}
