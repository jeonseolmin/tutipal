package com.tutipal.domain.screenshare.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class LiveKitTokensTest {

    JsonMapper json = JsonMapper.builder().build();
    LiveKitTokens tokens = new LiveKitTokens("key", "s".repeat(40), json,
            Clock.fixed(Instant.ofEpochSecond(1_000_000), ZoneOffset.UTC));

    JsonNode claims(String jwt) {
        return json.readTree(new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8));
    }

    @Test
    void signatureMatchesHeaderAndPayload() {
        String jwt = tokens.mint("u1", "선생님", new LiveKitTokens.Grant("session-1-class", true, true, false));
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);
        String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(tokens.hmac(parts[0] + "." + parts[1]));
        assertThat(parts[2]).isEqualTo(expected);
        assertThat(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)).contains("HS256");
    }

    @Test
    void claimsCarryRoomGrantsAndShortExpiry() {
        JsonNode c = claims(tokens.mint("u2", "학생", new LiveKitTokens.Grant("session-1-class", false, true, false)));
        assertThat(c.get("iss").asString()).isEqualTo("key");
        assertThat(c.get("sub").asString()).isEqualTo("u2");
        assertThat(c.get("exp").asLong() - 1_000_000).isEqualTo(LiveKitTokens.TTL.toSeconds());
        JsonNode v = c.get("video");
        assertThat(v.get("room").asString()).isEqualTo("session-1-class");
        assertThat(v.get("roomJoin").asBoolean()).isTrue();
        assertThat(v.get("canPublish").asBoolean()).isFalse();
        assertThat(v.get("canPublishData").asBoolean()).isFalse();
        assertThat(v.get("canPublishSources")).isEmpty();
    }

    @Test
    void publisherMayOnlyShareScreen() {
        JsonNode v = claims(tokens.mint("u1", "t", new LiveKitTokens.Grant("r", true, true, false))).get("video");
        assertThat(v.get("canPublish").asBoolean()).isTrue();
        assertThat(v.get("canPublishSources").get(0).asString()).isEqualTo("screen_share");
        assertThat(v.get("canPublishSources")).hasSize(1);
    }
}
