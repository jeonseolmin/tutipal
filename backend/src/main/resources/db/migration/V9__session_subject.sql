-- 세션 주제: 코딩 · 영어 · 수학 3개(enum 이름) → 과목 이름 자유 입력 (드롭다운 + 직접 입력, 최대 30자)
ALTER TABLE study_sessions ALTER COLUMN topic TYPE VARCHAR(30);

UPDATE study_sessions SET topic = CASE topic
    WHEN 'CODING' THEN '코딩'
    WHEN 'ENGLISH' THEN '영어'
    WHEN 'MATH' THEN '수학'
    WHEN 'GENERAL' THEN '자유'
    ELSE topic
END;
