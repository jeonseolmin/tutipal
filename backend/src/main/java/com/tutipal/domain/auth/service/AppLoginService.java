package com.tutipal.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.tutipal.global.exception.ApiException;

/**
 * 앱(Electron) 로그인: 시스템 브라우저에서 소셜 로그인 → 1회용 코드(Redis 60초) → tutipal:// 딥링크 → 앱이 세션 쿠키로 교환.
 * 앱이 처음에 만든 비밀값(verifier)의 해시(challenge)를 코드에 묶어 둬서, 다른 프로그램이 딥링크를 가로채도
 * verifier 가 없으면 교환할 수 없다 (RFC 8252 · PKCE 와 같은 방식).
 */
@Service
public class AppLoginService {

    /** base64url(SHA-256) = 43자 */
    static final Pattern CHALLENGE = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final Pattern VERIFIER = Pattern.compile("^[A-Za-z0-9_-]{43,128}$");
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final String KEY = "applogin:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    public AppLoginService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public static boolean validChallenge(String challenge) {
        return challenge != null && CHALLENGE.matcher(challenge).matches();
    }

    /** 브라우저 로그인 성공 직후: 1회용 코드 발급. */
    public String issue(Long userId, String challenge) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(KEY + code, userId + " " + challenge, TTL);
        return code;
    }

    /** 앱이 코드 + verifier 로 교환: 한 번만 쓸 수 있고, verifier 해시가 맞아야 사용자 id. */
    public Long exchange(String code, String verifier) {
        if (code == null || verifier == null || !CODE.matcher(code).matches() || !VERIFIER.matcher(verifier).matches()) {
            throw invalid();
        }
        String value = redis.opsForValue().getAndDelete(KEY + code);   // 맞든 틀리든 한 번 쓰면 사라짐
        if (value == null) {
            throw invalid();
        }
        String[] parts = value.split(" ", 2);
        byte[] expected = parts[1].getBytes(StandardCharsets.US_ASCII);
        byte[] actual = challengeOf(verifier).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw invalid();
        }
        return Long.valueOf(parts[0]);
    }

    static String challengeOf(String verifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "로그인 코드가 만료됐거나 맞지 않아요. 다시 로그인해 주세요");
    }
}
