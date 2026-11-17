package com.tutipal.domain.tutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.tutipal.global.exception.ApiException;

class TopicPromptTest {

    @Test
    void parsesTopicIgnoringCaseAndSpaces() {
        assertEquals(TopicPrompt.CODING, TopicPrompt.from("coding"));
        assertEquals(TopicPrompt.MATH, TopicPrompt.from(" MATH "));
    }

    @Test
    void everyPromptKeepsTheBaseRules() {
        for (TopicPrompt topic : TopicPrompt.values()) {
            assertTrue(topic.systemPrompt().contains("지어내지 않는다"), topic.name());
        }
        assertTrue(TopicPrompt.CODING.systemPrompt().contains("프로그래밍"));
    }

    @Test
    void unknownTopicIsBadRequest() {
        ApiException e = assertThrows(ApiException.class, () -> TopicPrompt.from("history"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertThrows(ApiException.class, () -> TopicPrompt.from(null));
    }
}
