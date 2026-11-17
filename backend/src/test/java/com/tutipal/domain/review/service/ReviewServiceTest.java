package com.tutipal.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tutipal.domain.review.repository.ReviewStore;
import com.tutipal.domain.review.repository.ReviewStore.Item;
import com.tutipal.global.ai.AiClient;

class ReviewServiceTest {

    static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    ReviewStore store = mock(ReviewStore.class);
    AiClient ai = mock(AiClient.class);
    ReviewService service = new ReviewService(store, ai, Clock.fixed(NOW, ZoneOffset.UTC));

    static Item item(int count, int seconds, Instant suggested) {
        return new Item(7L, "자바 for 문 코드", count, seconds, NOW.minus(Duration.ofDays(1)), NOW, suggested);
    }

    @BeforeEach
    void setUp() {
        when(store.consent(1L)).thenReturn(true);
        when(ai.embed(any())).thenReturn(List.of(new float[] {0.1f, 0.2f}));
    }

    @Test
    void noConsentNoRecord() {
        when(store.consent(2L)).thenReturn(false);
        assertThat(service.observe(2L, "화면", 40).recorded()).isFalse();
        verify(ai, never()).embed(any());
    }

    @Test
    void similarSummaryIsCountedAndThirdTimeSuggests() {
        when(store.nearest(eq(1L), anyString(), any(), anyDouble())).thenReturn(Optional.of(item(2, 80, null)));
        when(store.get(7L)).thenReturn(item(3, 120, null));
        var r = service.observe(1L, "자바 for 문", 40);
        verify(store).touch(7L, 1, 40, NOW, NOW.minus(ReviewService.WINDOW));
        assertThat(r.suggestion()).isNotNull();
        verify(store).markSuggested(7L, NOW);
    }

    @Test
    void newContentStartsNewGroupWithoutSuggestion() {
        when(store.nearest(eq(1L), anyString(), any(), anyDouble())).thenReturn(Optional.empty());
        when(store.insert(eq(1L), anyString(), anyString(), anyInt(), any())).thenReturn(8L);
        when(store.get(8L)).thenReturn(new Item(8L, "새 화면", 1, 40, NOW, NOW, null));
        assertThat(service.observe(1L, "새 화면", 40).suggestion()).isNull();
    }

    @Test
    void stillLookingAddsTimeAndTenMinutesSuggests() {
        when(store.latest(1L)).thenReturn(Optional.of(item(1, 580, null)));
        when(store.get(7L)).thenReturn(item(1, 620, null));
        var r = service.observe(1L, null, 999);   // 최대 120초로 자름
        verify(store).touch(7L, 0, 120, NOW, NOW.minus(ReviewService.WINDOW));
        assertThat(r.suggestion()).isNotNull();
    }

    @Test
    void stillLookingIgnoredAfterLongGap() {
        when(store.latest(1L)).thenReturn(Optional.of(new Item(7L, "s", 1, 0, NOW.minus(Duration.ofHours(1)), NOW.minus(Duration.ofMinutes(10)), null)));
        assertThat(service.observe(1L, null, 40).recorded()).isFalse();
        verify(store, never()).touch(anyLong(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void suggestsAtMostOncePerDay() {
        assertThat(service.suggestIfDue(item(5, 0, NOW.minus(Duration.ofHours(3))), NOW)).isNull();
        assertThat(service.suggestIfDue(item(5, 0, NOW.minus(Duration.ofHours(25))), NOW)).isNotNull();
        assertThat(service.suggestIfDue(item(2, 599, null), NOW)).isNull();
    }

    @Test
    void turningOffDeletesRecords() {
        service.setConsent(1L, false);
        verify(store).deleteAll(1L);
    }
}
