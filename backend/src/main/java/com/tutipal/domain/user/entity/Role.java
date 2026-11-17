package com.tutipal.domain.user.entity;

/**
 * 계정 역할. 선생님/학생은 계정이 아니라 세션마다 정해진다
 * (세션을 연 사람 = 그 세션의 선생님, studysession 도메인의 참가자 역할).
 */
public enum Role {
    ADMIN, USER
}
