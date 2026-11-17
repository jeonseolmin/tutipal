-- 공용 HNSW 인덱스는 "가까운 순으로 먼저 찾고 → 세션으로 거르기" 라서, 다른 세션 조각이 많으면 이 세션 결과가 빠진다 (코드 리뷰 지적).
-- 한 세션의 자료 조각은 많아야 수천 개라 session_id 인덱스로 거른 뒤 정확히 계산해도 충분히 빠르다.
-- ponytail: 세션당 조각이 수만 개가 되면 세션별 부분 인덱스나 hnsw.iterative_scan(pgvector 0.8+) 검토
DROP INDEX IF EXISTS idx_chunks_embedding;
