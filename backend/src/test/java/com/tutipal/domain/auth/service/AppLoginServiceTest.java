package com.tutipal.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.tutipal.global.exception.ApiException;

class AppLoginServiceTest {

    // RFC 7636 부록 B 의 예시 값
    static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    Map<String, String> store = new HashMap<>();
    AppLoginService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        doAnswer(inv -> store.put(inv.getArgument(0), inv.getArgument(1)))
                .when(ops).set(anyString(), anyString(), any(Duration.class));
        when(ops.getAndDelete(anyString())).thenAnswer(inv -> store.remove(inv.<String>getArgument(0)));
        service = new AppLoginService(redis);
    }

    @Test
    void challengeMatchesRfc7636Example() {
        assertThat(AppLoginService.challengeOf(VERIFIER)).isEqualTo(CHALLENGE);
        assertThat(AppLoginService.validChallenge(CHALLENGE)).isTrue();
        assertThat(AppLoginService.validChallenge("short")).isFalse();
    }

    @Test
    void codeWorksOnceWithRightVerifier() {
        String code = service.issue(7L, CHALLENGE);
        assertThat(service.exchange(code, VERIFIER)).isEqualTo(7L);
        assertThatThrownBy(() -> service.exchange(code, VERIFIER)).isInstanceOf(ApiException.class);   // 두 번째는 실패
    }

    @Test
    void wrongVerifierFailsAndBurnsCode() {
        String code = service.issue(7L, CHALLENGE);
        String other = "x".repeat(43);
        assertThatThrownBy(() -> service.exchange(code, other)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.exchange(code, VERIFIER)).isInstanceOf(ApiException.class);   // 가로챈 쪽이 먼저 쓰면 코드도 사라짐
    }

    @Test
    void rejectsMalformedInput() {
        assertThatThrownBy(() -> service.exchange("bad code", VERIFIER)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.exchange("a".repeat(43), "short")).isInstanceOf(ApiException.class);
    }
}
