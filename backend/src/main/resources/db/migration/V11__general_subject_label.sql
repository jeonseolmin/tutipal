-- V9 가 예전 GENERAL 주제를 '자유' 로 바꿨지만 화면 · 챗봇 지시문은 '자유 주제' 를 쓴다 → 맞춤
UPDATE study_sessions SET topic = '자유 주제' WHERE topic = '자유';
