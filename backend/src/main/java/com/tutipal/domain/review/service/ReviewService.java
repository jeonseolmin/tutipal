package com.tutipal.domain.review.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.tutipal.domain.review.repository.ReviewStore;
import com.tutipal.domain.review.repository.ReviewStore.Item;
import com.tutipal.global.ai.AiClient;
import com.tutipal.global.exception.ApiException;

/**
 * 복습 (목업 09 · 10). 위젯이 화면 요약을 받을 때마다 observe() 로 알려 준다.
 * 비슷한 요약(임베딩 유사도 0.7 이상, 최근 7일)은 한 묶음으로 세고, 3번 이상 보거나 10분 이상 보면 복습을 제안한다 (같은 묶음은 하루 한 번).
 * 결정: 2026-11-16 "3번 또는 10분". 동의(review_consent)한 사람만 기록한다.
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    static final int MIN_COUNT = 3;
    static final int MIN_SECONDS = 600;
    static final Duration WINDOW = Duration.ofDays(7);
    static final Duration SUGGEST_GAP = Duration.ofDays(1);
    /** 화면이 그대로일 때 시간만 더하는 건, 마지막으로 본 지 이만큼 안이면 (위젯 주기 40초의 몇 배) */
    static final Duration STILL_LOOKING = Duration.ofMinutes(3);
    /**
     * 같은 묶음으로 볼 유사도. 2026-11-16 bge-m3 실측: 같은 화면을 다르게 요약한 것끼리 0.72~0.83,
     * for 문 vs while 문(비슷하지만 다른 주제) 0.62~0.65, 관계없는 화면 0.52 이하 → 0.7.
     * ponytail: 여유가 좁음. 실제 기록이 쌓이면 다시 재 보고 조정
     */
    static final double SIMILAR = 0.7;

    public record Suggestion(Long id, String summary) {
    }

    public record Observed(boolean recorded, Suggestion suggestion) {
    }

    private final ReviewStore store;
    private final AiClient ai;
    private final Clock clock;

    @Autowired
    public ReviewService(ReviewStore store, AiClient ai) {
        this(store, ai, Clock.systemUTC());
    }

    ReviewService(ReviewStore store, AiClient ai, Clock clock) {
        this.store = store;
        this.ai = ai;
        this.clock = clock;
    }

    public boolean consent(Long userId) {
        return store.consent(userId);
    }

    /** 끄면 기록도 지운다 (동의를 거두면 남겨 둘 이유가 없음) */
    public void setConsent(Long userId, boolean on) {
        store.setConsent(userId, on);
        if (!on) {
            store.deleteAll(userId);
        }
    }

    /**
     * summary 가 있으면 새로 본 화면, 없으면 같은 화면을 계속 보는 중 (seconds 만 더함).
     * 실패해도 위젯 동작에는 영향 없게 recorded=false 로만 알린다.
     */
    public Observed observe(Long userId, String summary, int seconds) {
        if (!store.consent(userId)) {
            return new Observed(false, null);
        }
        int secs = Math.clamp(seconds, 0, 120);
        Instant now = clock.instant();
        try {
            Long id;
            if (summary == null || summary.isBlank()) {
                Item last = store.latest(userId).orElse(null);
                if (last == null || last.lastSeen().isBefore(now.minus(STILL_LOOKING))) {
                    return new Observed(false, null);
                }
                store.touch(last.id(), 0, secs, now, now.minus(WINDOW));
                id = last.id();
            } else {
                String text = summary.strip();
                text = text.length() > 500 ? text.substring(0, 500) : text;
                String vec = AiClient.pgVector(ai.embed(List.of(text)).getFirst());
                Item near = store.nearest(userId, vec, now.minus(WINDOW), SIMILAR).orElse(null);
                if (near != null) {
                    store.touch(near.id(), 1, secs, now, now.minus(WINDOW));
                    id = near.id();
                } else {
                    id = store.insert(userId, text, vec, secs, now);
                }
            }
            return new Observed(true, suggestIfDue(store.get(id), now));
        } catch (RuntimeException e) {
            log.warn("review observe failed: {}", e.getClass().getSimpleName());
            return new Observed(false, null);
        }
    }

    Suggestion suggestIfDue(Item item, Instant now) {
        boolean often = item.seenCount() >= MIN_COUNT || item.seenSeconds() >= MIN_SECONDS;
        boolean notToday = item.suggestedAt() == null || item.suggestedAt().isBefore(now.minus(SUGGEST_GAP));
        if (!often || !notToday) {
            return null;
        }
        store.markSuggested(item.id(), now);
        return new Suggestion(item.id(), item.summary());
    }

    public List<Item> list(Long userId) {
        return store.list(userId);
    }

    public Item get(Long userId, Long id) {
        return store.find(userId, id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "복습 기록을 찾을 수 없어요"));
    }

    public void deleteAll(Long userId) {
        store.deleteAll(userId);
    }

    public void delete(Long userId, Long id) {
        store.delete(userId, id);
    }

}
