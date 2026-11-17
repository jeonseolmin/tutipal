package com.tutipal.domain.tutor;

import java.util.Locale;

import org.springframework.http.HttpStatus;

import com.tutipal.global.exception.ApiException;

/** 학습 주제별 도우미 챗봇 지시문. 세션 단계에서는 선생님 설정이 여기에 덧붙는다. */
public enum TopicPrompt {

    GENERAL("일반", ""),
    CODING("코딩", """
            지금 주제는 프로그래밍이다. 개념을 먼저 짧게 설명하고, 필요하면 짧은 코드 예시를 코드 블록으로 보여 준다.
            오류 메시지를 받으면 원인과 고치는 방법을 단계별로 알려 준다."""),
    ENGLISH("영어", """
            지금 주제는 영어 학습이다. 영어 문장은 자연스러운 표현과 함께 한국어 뜻을 알려 주고,
            틀린 부분이 있으면 고친 문장과 이유를 짧게 설명한다."""),
    MATH("수학", """
            지금 주제는 수학이다. 답만 주지 말고 풀이 과정을 단계별로 보여 주며, 마지막에 답을 정리한다.""");

    private static final String BASE = """
            너는 학생을 돕는 친절한 AI 학습 도우미 TUTIPAL 이다. 한국어로 쉽고 정확하게 답한다.
            모르는 것은 모른다고 말하고 지어내지 않는다. 답은 필요한 만큼만 짧게 한다.""";

    private final String label;
    private final String extra;

    TopicPrompt(String label, String extra) {
        this.label = label;
        this.extra = extra;
    }

    public String label() {
        return label;
    }

    public String systemPrompt() {
        return extra.isEmpty() ? BASE : BASE + "\n" + extra;
    }

    public static TopicPrompt from(String topic) {
        try {
            return valueOf(topic.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 주제예요: " + topic);
        }
    }
}
