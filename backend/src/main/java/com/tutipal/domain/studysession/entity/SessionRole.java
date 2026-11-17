package com.tutipal.domain.studysession.entity;

/** 세션 안에서의 역할 (계정 역할 Role 과 별개). 세션을 연 사람만 TEACHER. */
public enum SessionRole {
    TEACHER, STUDENT
}
