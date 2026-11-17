package com.tutipal.domain.qna.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tutipal.domain.qna.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdOrderByIdAsc(Long postId);

    List<Comment> findByPostIdIn(Collection<Long> postIds);
}
