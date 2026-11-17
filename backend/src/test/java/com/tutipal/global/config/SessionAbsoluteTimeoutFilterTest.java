package com.tutipal.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class SessionAbsoluteTimeoutFilterTest {

    private MockHttpSession runAfter(Duration elapsed) throws Exception {
        MockHttpSession session = new MockHttpSession();
        Instant now = Instant.ofEpochMilli(session.getCreationTime()).plus(elapsed);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new SessionAbsoluteTimeoutFilter(Clock.fixed(now, ZoneOffset.UTC))
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return session;
    }

    @Test
    void keepsSessionYoungerThan12Hours() throws Exception {
        assertThat(runAfter(Duration.ofHours(11)).isInvalid()).isFalse();
    }

    @Test
    void invalidatesSessionOlderThan12Hours() throws Exception {
        assertThat(runAfter(Duration.ofHours(12).plusSeconds(1)).isInvalid()).isTrue();
    }

    @Test
    void passesRequestWithoutSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        new SessionAbsoluteTimeoutFilter(Clock.systemUTC())
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(request.getSession(false)).isNull();
    }
}
