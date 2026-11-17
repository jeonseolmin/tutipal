"""DB 스키마 덤프(schema.txt) → Mermaid ERD (docs/erd/ERD.md) + 출력용 HTML."""
import collections
import os
import sys

SRC, OUTDIR, HTML = sys.argv[1], sys.argv[2], sys.argv[3]

TABLE_KO = {
    'users': '사용자 (소셜 로그인)', 'study_sessions': '방 (단체 학습)', 'session_participants': '방 참가자 (방장/참여자, 즐겨찾기)',
    'quizzes': '이해도 질문', 'quiz_answers': '질문 답변 · 채점', 'help_requests': '도움 요청',
    'materials': '수업 자료 (파일)', 'material_chunks': '자료 조각 + 임베딩 (RAG)', 'notices': '공지',
    'posts': '게시판 글', 'comments': '댓글', 'review_items': '복습 기록 (화면 요약 + 임베딩)',
}
COL_KO = {
    ('users', 'provider'): 'kakao / naver / google / dev', ('users', 'review_consent'): '복습 기록 동의', ('users', 'role'): 'USER / ADMIN',
    ('study_sessions', 'topic'): '과목 이름 (직접 입력 가능)', ('study_sessions', 'invite_code'): '초대 코드 6자리',
    ('study_sessions', 'owner_id'): '방장', ('study_sessions', 'difficulty'): '챗봇 난이도 1~5',
    ('study_sessions', 'ended_at'): '방 닫힌 시각',
    ('session_participants', 'role'): 'TEACHER / STUDENT', ('session_participants', 'pinned'): '홈 즐겨찾기',
    ('quizzes', 'type'): 'CHOICE / TEXT', ('quiz_answers', 'graded_by'): 'AUTO / AI / TEACHER',
    ('help_requests', 'status'): 'PENDING / ACCEPTED / DECLINED / DONE', ('materials', 'status'): 'PROCESSING / DONE / FAILED',
    ('material_chunks', 'embedding'): 'vector(1024) bge-m3', ('material_chunks', 'session_id'): '검색용 복사 (FK 없음)',
    ('review_items', 'embedding'): 'vector(1024) bge-m3', ('review_items', 'suggested_at'): '복습 제안한 시각',
}


def typ(t, n):
    t = {'bigint': 'bigint', 'integer': 'int', 'smallint': 'smallint', 'boolean': 'boolean', 'text': 'text',
         'bytea': 'bytea', 'timestamp with time zone': 'timestamptz', 'ARRAY': 'text_array',
         'USER-DEFINED': 'vector', 'character varying': 'varchar'}.get(t, t.replace(' ', '_'))
    return f'{t}({n})' if n else t


cols = collections.OrderedDict()
fks = []
for line in open(SRC, encoding='utf-8'):
    table, col, t, n, nullable, pk, fk, uk = line.rstrip('\n').split('|')
    keys = [k for k in (pk, 'FK' if fk else '', uk) if k]
    note = COL_KO.get((table, col), '' if nullable == 'NO' else 'null 가능')
    if note and nullable == 'YES' and 'null' not in note:
        note += ', null 가능'
    cols.setdefault(table, []).append((typ(t, n), col, ','.join(keys), note))
    if fk:
        fks.append((fk, table, col))

uniq = {('session_participants', 'session_id'), ('quiz_answers', 'quiz_id')}
lines = ['erDiagram']
for parent, child, col in fks:
    lines.append(f'    {parent} ||--o{{ {child} : "{col}"')
for table, cs in cols.items():
    lines.append(f'    {table} {{')
    for t, c, k, note in cs:
        lines.append(f'        {t} {c}' + (f' {k}' if k else '') + (f' "{note}"' if note else ''))
    lines.append('    }')
mermaid = '\n'.join(lines)

legend = '\n'.join(f'| `{t}` | {TABLE_KO.get(t, "")} |' for t in cols)
md = f"""# ERD

- 논리 ERD: `ERD-logical.png` / `.pdf` — 한국어 엔터티 · 속성, 식별자와 관계(1:N, 사용자–방 N:M 은 ‘방 참가’로 풂)만
- 물리 ERD: `ERD-physical.png` / `.pdf`, 아래 Mermaid — 실제 테이블 · 컬럼 · 타입 · 키

실제 DB(Flyway V1~V10 적용 후)에서 뽑았다. Flyway · Spring Session 테이블은 뺐다. 다시 뽑을 때는 이 그림들을 새로 만든다 (DB 를 바꾸면 같이 갱신).
같은 사람이 같은 방에 두 번 들어갈 수 없다 (`session_participants` 의 session_id+user_id 유니크), 질문 하나에 답은 한 사람당 하나 (`quiz_answers` 의 quiz_id+user_id 유니크).

| 테이블 | 뜻 |
|---|---|
{legend}

```mermaid
{mermaid}
```
"""
os.makedirs(OUTDIR, exist_ok=True)
open(os.path.join(OUTDIR, 'ERD.md'), 'w', encoding='utf-8').write(md)

rows = ''.join(f'<tr><td><code>{t}</code></td><td>{TABLE_KO.get(t, "")}</td></tr>' for t in cols)
html = f"""<!doctype html><meta charset="utf-8">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;700&display=swap">
<style>
@page {{ size: 2400px 1700px; margin: 0 }}
body {{ margin: 0; padding: 40px 48px; font-family: 'Noto Sans KR', sans-serif; color: #1F2633; background: #FBFCFE; width: max-content; }}
h1 {{ font-size: 40px; margin: 0 0 6px }} p {{ font-size: 20px; color: #4A5568; margin: 0 0 18px }}
.wrap {{ display: flex; flex-direction: column; gap: 24px }}
table {{ border-collapse: collapse; font-size: 18px; display: grid; grid-template-columns: repeat(4, auto); justify-content: start; column-gap: 24px }} tbody {{ display: contents }} tr {{ display: contents }} td {{ border-bottom: 1px solid #D5DEEA; padding: 6px 12px }}
.mermaid {{ flex: 1 }}
</style>
<h1>TUTIPAL 물리 ERD</h1>
<p>Postgres · Flyway V1~V10 기준 (2026-11-17). 모든 컬럼 · 타입 · PK/FK/UK, 설명칸의 ‘null 가능’ 외에는 NOT NULL. 선 이름 = 외래키 컬럼.</p>
<div class="wrap"><table>{rows}</table><pre class="mermaid">
{mermaid}
</pre></div>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script>
<script>mermaid.initialize({{ startOnLoad: true, theme: 'neutral', er: {{ fontSize: 16, useMaxWidth: false }}, fontFamily: 'Noto Sans KR, sans-serif' }});</script>
"""
open(HTML, 'w', encoding='utf-8').write(html)
print(len(cols), 'tables', len(fks), 'fks')
