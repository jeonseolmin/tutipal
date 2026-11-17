package com.tutipal.domain.studysession.dto;


/** 세션 챗봇에 적용할 선생님 설정 (tutor 도메인이 지시문을 만들 때 씀). */
public record ChatSettings(String topic, int difficulty, boolean hideAnswers, boolean preferMaterials) {
}
