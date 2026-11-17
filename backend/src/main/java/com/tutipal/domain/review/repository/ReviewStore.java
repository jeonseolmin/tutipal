package com.tutipal.domain.review.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 복습 기록 저장소. 임베딩(pgvector) 비교가 필요해서 JPA 대신 JDBC.
 * review_items 한 줄 = 비슷한 화면 요약들의 묶음 (본 횟수 · 본 시간 누적).
 */
@Repository
public class ReviewStore {

    public record Item(Long id, String summary, int seenCount, int seenSeconds, Instant firstSeen, Instant lastSeen,
                       Instant suggestedAt) {
    }

    private static final String COLS = "id, summary, seen_count, seen_seconds, first_seen, last_seen, suggested_at";
    private static final RowMapper<Item> MAPPER = (rs, i) -> new Item(rs.getLong(1), rs.getString(2), rs.getInt(3),
            rs.getInt(4), rs.getTimestamp(5).toInstant(), rs.getTimestamp(6).toInstant(),
            rs.getTimestamp(7) == null ? null : rs.getTimestamp(7).toInstant());

    private final JdbcTemplate jdbc;

    public ReviewStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean consent(Long userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT review_consent FROM users WHERE id = ?", Boolean.class, userId));
    }

    public void setConsent(Long userId, boolean on) {
        jdbc.update("UPDATE users SET review_consent = ? WHERE id = ?", on, userId);
    }

    /** since 이후에 본 묶음 중 가장 비슷한 것 (유사도 minSimilarity 이상) */
    public Optional<Item> nearest(Long userId, String vector, Instant since, double minSimilarity) {
        return jdbc.query("SELECT " + COLS + ", 1 - (embedding <=> ?::vector) AS sim FROM review_items "
                        + "WHERE user_id = ? AND last_seen >= ? ORDER BY embedding <=> ?::vector LIMIT 1",
                        (rs, i) -> rs.getDouble(8) >= minSimilarity ? MAPPER.mapRow(rs, i) : null,
                        vector, userId, Timestamp.from(since), vector)
                .stream().filter(java.util.Objects::nonNull).findFirst();
    }

    public Optional<Item> latest(Long userId) {
        return jdbc.query("SELECT " + COLS + " FROM review_items WHERE user_id = ? ORDER BY last_seen DESC LIMIT 1", MAPPER, userId)
                .stream().findFirst();
    }

    public Long insert(Long userId, String summary, String vector, int seconds, Instant now) {
        return jdbc.queryForObject("INSERT INTO review_items (user_id, summary, embedding, seen_count, seen_seconds, first_seen, last_seen) "
                + "VALUES (?, ?, ?::vector, 1, ?, ?, ?) RETURNING id", Long.class, userId, summary, vector, seconds, Timestamp.from(now), Timestamp.from(now));
    }

    /**
     * 다시 봄: 횟수 +countDelta, 시간 +seconds. 묶음의 세기 시작(first_seen)이 windowStart 보다 오래됐으면
     * 그 창은 끝난 것 → 이번 것부터 새로 센다 ("최근 7일 안 3번 · 10분" 을 지키려고).
     * ponytail: 본 시각을 하나하나 저장하지 않는 근사 (창이 묶음마다 이어서 7일씩). 정확히 하려면 본 기록 테이블을 따로
     */
    public void touch(Long id, int countDelta, int seconds, Instant now, Instant windowStart) {
        Timestamp start = Timestamp.from(windowStart);
        jdbc.update("""
                UPDATE review_items SET
                  seen_count   = CASE WHEN first_seen < ? THEN GREATEST(?, 1) ELSE seen_count + ? END,
                  seen_seconds = CASE WHEN first_seen < ? THEN ? ELSE seen_seconds + ? END,
                  first_seen   = CASE WHEN first_seen < ? THEN ? ELSE first_seen END,
                  last_seen    = ?
                WHERE id = ?""",
                start, countDelta, countDelta, start, seconds, seconds, start, Timestamp.from(now), Timestamp.from(now), id);
    }

    public Item get(Long id) {
        return jdbc.queryForObject("SELECT " + COLS + " FROM review_items WHERE id = ?", MAPPER, id);
    }

    public void markSuggested(Long id, Instant now) {
        jdbc.update("UPDATE review_items SET suggested_at = ? WHERE id = ?", Timestamp.from(now), id);
    }

    public List<Item> list(Long userId) {
        return jdbc.query("SELECT " + COLS + " FROM review_items WHERE user_id = ? ORDER BY seen_count + seen_seconds / 120 DESC, last_seen DESC LIMIT 100",
                MAPPER, userId);
    }

    public Optional<Item> find(Long userId, Long id) {
        return jdbc.query("SELECT " + COLS + " FROM review_items WHERE user_id = ? AND id = ?", MAPPER, userId, id).stream().findFirst();
    }

    public int deleteAll(Long userId) {
        return jdbc.update("DELETE FROM review_items WHERE user_id = ?", userId);
    }

    public int delete(Long userId, Long id) {
        return jdbc.update("DELETE FROM review_items WHERE user_id = ? AND id = ?", userId, id);
    }
}
