package com.tutipal.domain.screenshare.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * LiveKit 입장권 (JWT HS256). JWT 는 여기서만 쓴다 (로그인은 세션 쿠키).
 * LiveKit 은 우리 세션 장부를 못 보므로, Spring 이 권한을 확인한 뒤 "이 방에서 이것만 해도 됨" 을 서명해 준다.
 * 입장할 때만 검사하므로 짧게(10분). SDK 없이 표준 라이브러리 HMAC 으로 만든다 (모양이 단순해서).
 */
@Component
public class LiveKitTokens {

    static final Duration TTL = Duration.ofMinutes(10);

    /** canPublishScreen = 화면 공유 트랙만 올릴 수 있음 (카메라 · 마이크 불가). */
    public record Grant(String room, boolean canPublishScreen, boolean canSubscribe, boolean canPublishData) {
    }

    private final String apiKey;
    private final byte[] secret;
    private final JsonMapper json;
    private final Clock clock;

    @Autowired
    public LiveKitTokens(@Value("${app.livekit.api-key}") String apiKey,
                         @Value("${app.livekit.api-secret}") String apiSecret,
                         JsonMapper json) {
        this(apiKey, apiSecret, json, Clock.systemUTC());
    }

    LiveKitTokens(String apiKey, String apiSecret, JsonMapper json, Clock clock) {
        this.apiKey = apiKey;
        this.secret = apiSecret.getBytes(StandardCharsets.UTF_8);
        this.json = json;
        this.clock = clock;
    }

    public String mint(String identity, String name, Grant grant) {
        long now = clock.instant().getEpochSecond();
        Map<String, Object> video = new LinkedHashMap<>();
        video.put("room", grant.room());
        video.put("roomJoin", true);
        video.put("canPublish", grant.canPublishScreen());
        video.put("canPublishSources", grant.canPublishScreen() ? List.of("screen_share") : List.of());
        video.put("canSubscribe", grant.canSubscribe());
        video.put("canPublishData", grant.canPublishData());
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", apiKey);
        claims.put("sub", identity);
        claims.put("name", name);
        claims.put("nbf", now - 5);
        claims.put("exp", now + TTL.toSeconds());
        claims.put("video", video);
        String body = b64(json.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT"))) + "." + b64(json.writeValueAsBytes(claims));
        return body + "." + b64(hmac(body));
    }

    byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
