package com.tutipal.domain.user.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SocialProfileTest {

    @Test
    void google() {
        var p = SocialProfile.of("google", Map.of("sub", "g1", "email", "a@g.com", "name", "구글"));
        assertEquals(new SocialProfile("google", "g1", "a@g.com", "구글"), p);
    }

    @Test
    void naverNestsUnderResponse() {
        var p = SocialProfile.of("naver", Map.of("response", Map.of("id", "n1", "email", "b@n.com", "name", "네이버")));
        assertEquals(new SocialProfile("naver", "n1", "b@n.com", "네이버"), p);
    }

    @Test
    void kakaoNumericIdAndNickname() {
        var p = SocialProfile.of("kakao", Map.of("id", 12345L,
                "kakao_account", Map.of("email", "c@k.com", "profile", Map.of("nickname", "카카오"))));
        assertEquals(new SocialProfile("kakao", "12345", "c@k.com", "카카오"), p);
    }

    @Test
    void kakaoWithoutConsentFallsBackToDefaultName() {
        var p = SocialProfile.of("kakao", Map.of("id", 7L));
        assertEquals(null, p.email());
        assertEquals("kakao 사용자", p.displayName());
    }

    @Test
    void unknownProviderRejected() {
        assertThrows(IllegalArgumentException.class, () -> SocialProfile.of("github", Map.of()));
    }

    @Test
    void missingProviderIdRejected() {
        assertThrows(IllegalArgumentException.class, () -> SocialProfile.of("naver", Map.of("response", Map.of())));
        assertThrows(IllegalArgumentException.class, () -> SocialProfile.of("google", Map.of("sub", " ")));
    }

    @Test
    void longNameIsCutToColumnLengthByCharacter() {
        String longName = "가".repeat(49) + "😀😀";      // 51 글자 (이모지는 2 char)
        var p = new SocialProfile("google", "g1", null, longName);
        String shown = p.displayName();
        assertEquals(50, shown.codePointCount(0, shown.length()));
        assertEquals("가".repeat(49) + "😀", shown);
    }
}
