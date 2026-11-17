-- 홈 방 목록 즐겨찾기: 사람마다 · 방마다 (켜면 그 칸 맨 위에 고정)
ALTER TABLE session_participants ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
