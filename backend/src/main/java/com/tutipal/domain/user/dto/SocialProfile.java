package com.tutipal.domain.user.dto;

import java.util.Map;

/** 소셜 로그인 응답(제공자마다 모양이 다름) → 공통 형태. */
public record SocialProfile(String provider, String providerId, String email, String name) {

    /** users.name 컬럼 길이 */
    static final int NAME_MAX = 50;

    public SocialProfile {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("소셜 응답에 사용자 id 가 없음: " + provider);
        }
    }

    @SuppressWarnings("unchecked")
    public static SocialProfile of(String provider, Map<String, Object> attrs) {
        return switch (provider) {
            case "google" -> new SocialProfile(provider,
                    str(attrs.get("sub")), str(attrs.get("email")), str(attrs.get("name")));
            case "naver" -> {
                var r = (Map<String, Object>) attrs.getOrDefault("response", Map.of());
                yield new SocialProfile(provider, str(r.get("id")), str(r.get("email")), str(r.get("name")));
            }
            case "kakao" -> {
                var account = (Map<String, Object>) attrs.getOrDefault("kakao_account", Map.of());
                var profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
                yield new SocialProfile(provider,
                        str(attrs.get("id")), str(account.get("email")), str(profile.get("nickname")));
            }
            default -> throw new IllegalArgumentException("지원하지 않는 로그인: " + provider);
        };
    }

    /** 화면에 보일 이름: 없으면 기본값, 길면 컬럼 길이에 맞춰 자름 (글자 단위). */
    public String displayName() {
        String n = name == null || name.isBlank() ? provider + " 사용자" : name.strip();
        return n.codePointCount(0, n.length()) <= NAME_MAX ? n : n.substring(0, n.offsetByCodePoints(0, NAME_MAX));
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
