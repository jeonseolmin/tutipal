package com.tutipal.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.tutipal.domain.quiz.entity.QuizType;

import tools.jackson.databind.json.JsonMapper;

class QuizAiTest {

    QuizAi ai = new QuizAi(null, null, JsonMapper.builder().build());

    @Test
    void parsesGrade() {
        var g = ai.parseGrade("{\"correct\": false, \"feedback\": \"핵심이 빠졌어요\"}");
        assertThat(g.correct()).isFalse();
        assertThat(g.feedback()).isEqualTo("핵심이 빠졌어요");
    }

    @Test
    void rejectsGradeWithoutBoolean() {
        assertThatThrownBy(() -> ai.parseGrade("{\"correct\": \"yes\"}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ai.parseGrade("정답입니다")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesChoiceDraftAndChecksAnswerIndex() {
        var d = ai.parseDraft("{\"question\":\"2+2?\",\"choices\":[\"3\",\"4\",\"5\",\"6\"],\"answerIndex\":1}", QuizType.CHOICE);
        assertThat(d.choices()).hasSize(4);
        assertThat(d.answerIndex()).isEqualTo(1);
        assertThatThrownBy(() -> ai.parseDraft("{\"question\":\"q\",\"choices\":[\"a\",\"b\"],\"answerIndex\":4}", QuizType.CHOICE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesTextDraft() {
        var d = ai.parseDraft("{\"question\":\"변수란?\",\"modelAnswer\":\"값을 담는 이름\"}", QuizType.TEXT);
        assertThat(d.modelAnswer()).isEqualTo("값을 담는 이름");
        assertThatThrownBy(() -> ai.parseDraft("{\"question\":\"변수란?\"}", QuizType.TEXT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
