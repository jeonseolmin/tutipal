package com.tutipal.domain.notice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tutipal.domain.notice.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findBySessionIdOrderByIdDesc(Long sessionId);
}
