package com.tutipal.domain.studysession.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InviteCodesTest {

    /** 회귀: 정규식이 "[\s-]"(공백 한 글자)로 잘못 들어가 탭 · 줄바꿈이 남았던 적 있음 */
    @Test
    void normalizeRemovesAllWhitespaceAndHyphens() {
        assertThat(InviteCodes.normalize(" a7k-2q9 ")).isEqualTo("A7K2Q9");
        assertThat(InviteCodes.normalize("A7K2Q9\t")).isEqualTo("A7K2Q9");
        assertThat(InviteCodes.normalize("A7K2\nQ9")).isEqualTo("A7K2Q9");
    }
}
