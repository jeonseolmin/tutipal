"""논리 ERD: 한국어 엔터티 · 속성, 타입 없이 식별자와 관계(1:N, 방 참가로 풀린 N:M)만. HTML+SVG 로 그려 헤드리스 Chrome 으로 찍는다."""
import json
import sys

OUT = sys.argv[1]

# 이름, 위치(x, y), 속성 [(표시, 이름)] — 표시: PK / FK / ''
E = {
    'user': ('사용자', (1060, 60), [('PK', '사용자번호'), ('', '이메일'), ('', '이름'), ('', '로그인 방식'), ('', '소셜 계정 ID'),
                                ('', '권한'), ('', '복습 기록 동의 여부'), ('', '가입 일시')]),
    'review': ('복습 기록', (120, 60), [('PK', '기록번호'), ('FK', '사용자번호'), ('', '화면 요약'), ('', '요약 임베딩'), ('', '본 횟수'),
                                    ('', '본 시간(초)'), ('', '처음 본 일시'), ('', '마지막 본 일시'), ('', '복습 제안 일시')]),
    'room': ('방', (1500, 620), [('PK', '방번호'), ('FK', '방장 사용자번호'), ('', '방 이름'), ('', '과목'), ('', '초대 코드'),
                               ('', '챗봇 난이도'), ('', '정답 숨김 여부'), ('', '자료 근거 우선 여부'), ('', '닫힌 일시')]),
    'member': ('방 참가', (1520, 110), [('PK', '참가번호'), ('FK', '방번호'), ('FK', '사용자번호'), ('', '역할 (방장/참여자)'),
                                    ('', '즐겨찾기 여부'), ('', '참여 일시')]),
    'help': ('도움 요청', (120, 620), [('PK', '요청번호'), ('FK', '방번호'), ('FK', '사용자번호'), ('', '메시지'), ('', '화면 공유 여부'), ('', '상태')]),
    'quiz': ('이해도 질문', (2240, 200), [('PK', '질문번호'), ('FK', '방번호'), ('', '유형 (객관식/주관식)'), ('', '질문 내용'),
                                     ('', '보기 목록'), ('', '정답 번호'), ('', '모범 답안'), ('', '마감 일시')]),
    'answer': ('질문 답변', (2240, 820), [('PK', '답변번호'), ('FK', '질문번호'), ('FK', '사용자번호'), ('', '고른 보기'), ('', '답변 내용'),
                                     ('', '정답 여부'), ('', '채점 주체'), ('', '피드백')]),
    'material': ('수업 자료', (1840, 1260), [('PK', '자료번호'), ('FK', '방번호'), ('FK', '올린 사용자번호'), ('', '파일 이름'), ('', '크기'),
                                        ('', '파일 내용'), ('', '처리 상태'), ('', '조각 수')]),
    'chunk': ('자료 조각', (2400, 1500), [('PK', '조각번호'), ('FK', '자료번호'), ('', '쪽 번호'), ('', '내용'), ('', '내용 임베딩')]),
    'notice': ('공지', (1260, 1320), [('PK', '공지번호'), ('FK', '방번호'), ('FK', '작성자번호'), ('', '제목'), ('', '내용'), ('', '작성 일시')]),
    'post': ('게시글', (700, 1320), [('PK', '글번호'), ('FK', '방번호'), ('FK', '작성자번호'), ('', '제목'), ('', '내용'), ('', '작성 일시')]),
    'comment': ('댓글', (140, 1320), [('PK', '댓글번호'), ('FK', '글번호'), ('FK', '작성자번호'), ('', '내용'), ('', '작성 일시')]),
}
# (1쪽, N쪽, 관계 이름, 사용자 관계면 True → 회색)
R = [
    ('user', 'room', '만든다 (방장)', False),
    ('user', 'member', '참가한다', True), ('room', 'member', '참가자를 둔다', False),
    ('user', 'review', '쌓는다', True),
    ('room', 'quiz', '낸다', False), ('quiz', 'answer', '받는다', False), ('user', 'answer', '답한다', True),
    ('room', 'help', '받는다', False), ('user', 'help', '요청한다', True),
    ('room', 'material', '가진다', False), ('user', 'material', '올린다', True, [1300, 1200]), ('material', 'chunk', '나뉜다', False),
    ('room', 'notice', '올린다', False), ('user', 'notice', '쓴다', True),
    ('room', 'post', '가진다', False), ('user', 'post', '쓴다', True),
    ('post', 'comment', '달린다', False), ('user', 'comment', '쓴다', True),
]

boxes = ''
for key, (name, (x, y), attrs) in E.items():
    rows = ''.join(f'<li class="{k.lower()}"><span class="k">{k}</span>{a}</li>' for k, a in attrs)
    boxes += f'<div class="ent" id="{key}" style="left:{x}px;top:{y}px"><h3>{name}</h3><ul>{rows}</ul></div>'

html = f"""<!doctype html><meta charset="utf-8">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;700&display=swap">
<style>
body {{ margin: 0; background: #FBFCFE; font-family: 'Noto Sans KR', sans-serif; color: #1F2633; }}
#c {{ position: relative; width: 2780px; height: 1820px; }}
h1 {{ position: absolute; left: 120px; top: 1700px; margin: 0; font-size: 34px; }}
.legend {{ position: absolute; left: 520px; top: 1708px; font-size: 20px; color: #4A5568; }}
svg {{ position: absolute; left: 0; top: 0; }}
.ent {{ position: absolute; width: 300px; background: #fff; border: 2px solid #1F2633; border-radius: 10px; overflow: hidden; }}
.ent h3 {{ margin: 0; padding: 8px 12px; font-size: 22px; background: #1A73F0; color: #fff; text-align: center; }}
.ent ul {{ list-style: none; margin: 0; padding: 6px 0; }}
.ent li {{ font-size: 18px; padding: 2px 12px; display: flex; gap: 8px; }}
.ent li.pk {{ font-weight: 700; border-bottom: 1px solid #D5DEEA; padding-bottom: 6px; margin-bottom: 4px; }}
.k {{ width: 30px; flex: none; color: #1A73F0; font-weight: 700; font-size: 15px; padding-top: 2px; }}
li.fk .k {{ color: #C2410C; }}
.lab {{ font-size: 16px; fill: #1F2633; paint-order: stroke; stroke: #FBFCFE; stroke-width: 6px; }}
</style>
<div id="c"><svg id="s" width="2780" height="1820"></svg>{boxes}
<h1>TUTIPAL 논리 ERD</h1>
<p class="legend">PK 식별자 · FK 참조 · 선 끝 ‖ = 1, 까마귀발 = N · 파란 선: 방 관계, 회색 선: 사용자 관계 · 사용자–방 N:M 은 ‘방 참가’로 풂</p></div>
<script>
const R = {json.dumps(R, ensure_ascii=False)};
const s = document.getElementById('s'), NS = 'http://www.w3.org/2000/svg';
const rect = id => {{ const e = document.getElementById(id); return {{x: e.offsetLeft, y: e.offsetTop, w: e.offsetWidth, h: e.offsetHeight}}; }};
const edge = (r, tx, ty) => {{ // 박스 중심에서 (tx,ty) 쪽으로 나가는 선이 박스 테두리와 만나는 점
  const cx = r.x + r.w / 2, cy = r.y + r.h / 2, dx = tx - cx, dy = ty - cy;
  const k = Math.min(Math.abs((r.w / 2) / (dx || 1e-9)), Math.abs((r.h / 2) / (dy || 1e-9)));
  return [cx + dx * k, cy + dy * k];
}};
const add = (tag, a) => {{ const e = document.createElementNS(NS, tag); for (const k in a) e.setAttribute(k, a[k]); s.appendChild(e); return e; }};
const unit = (x, y) => {{ const L = Math.hypot(x, y); return [x / L, y / L]; }};
for (const [one, many, label, user, via] of R) {{
  // via: 다른 박스를 비켜 가는 곡선의 조절점 (없으면 직선)
  const a = rect(one), b = rect(many);
  const ac = [a.x + a.w / 2, a.y + a.h / 2], bc = [b.x + b.w / 2, b.y + b.h / 2];
  const [x1, y1] = edge(a, ...(via || bc)), [x2, y2] = edge(b, ...(via || ac));
  const col = user ? '#9AA5B5' : '#1A73F0';
  const cx = via ? via[0] : (x1 + x2) / 2, cy = via ? via[1] : (y1 + y2) / 2;
  add('path', {{d: `M${{x1}} ${{y1}} Q${{cx}} ${{cy}} ${{x2}} ${{y2}}`, fill: 'none', stroke: col, 'stroke-width': 2.5}});
  let [ux, uy] = unit(cx - x1, cy - y1);
  for (const d of [14, 22]) {{ const mx = x1 + ux * d, my = y1 + uy * d; add('line', {{x1: mx - uy * 10, y1: my + ux * 10, x2: mx + uy * 10, y2: my - ux * 10, stroke: col, 'stroke-width': 2.5}}); }}
  [ux, uy] = unit(x2 - cx, y2 - cy);
  const bx = x2 - ux * 22, by = y2 - uy * 22;
  for (const w of [-12, 0, 12]) add('line', {{x1: bx, y1: by, x2: x2 - uy * w, y2: y2 + ux * w, stroke: col, 'stroke-width': 2.5}});
  const t = add('text', {{x: 0.25 * x1 + 0.5 * cx + 0.25 * x2, y: 0.25 * y1 + 0.5 * cy + 0.25 * y2 - 6, 'text-anchor': 'middle', class: 'lab'}}); t.textContent = label;
}}
</script>"""
open(OUT, 'w', encoding='utf-8').write(html)
print('ok')
