# docs — 설계 자료

| 폴더 | 무엇 | 다시 만들기 |
|---|---|---|
| `design/` | 설계 문서 PDF 사본(최신은 Claude Docs, 링크는 HANDOFF), 세션 방식 보안 근거 | — |
| `architecture/` | 전체 구조 · 디렉토리/MVC · 화면 공유 · 로그인 · 데스크톱 기술 선택 그림 (`.svg` 원본 + `.png`) | svg 를 고치고 png 로 내보내기 |
| `erd/` | 논리 ERD · 물리 ERD (`.png` · `.pdf`), `ERD.md` (물리 ERD Mermaid, GitHub 에서 그림으로 보임) | `python docs/erd/source/render.py` (docker compose 켠 상태) |
| `storyboard/` | 화면 설계 스토리보드 PDF, 캡처 원본 `screens/`, 슬라이드 원본 `deck/` | 캡처 `source/capture.py` → PDF `source/render.py` |
| `mockups/` | 화면 목업 `screens/01~32`, `widgets/` (구현할 때 번호로 찾는 디자인 기준) | — |

- DB 를 바꾸면(Flyway 마이그레이션 추가) ERD 를 다시 만든다. 논리 ERD 는 `erd/source/logical.py` 의 엔터티 · 관계 목록을 손으로 고친 뒤 실행
- 화면을 크게 바꾸면 스토리보드 캡처도 다시. 슬라이드 원본은 Claude Artifact 덱(링크는 HANDOFF)과 같은 파일
- 스크립트는 Windows + Chrome 기준 (Chrome 경로는 `CHROME` 환경변수로 바꿀 수 있음), Pillow 필요
- 지금 스토리보드 캡처는 예전 초록 부엉이 캐릭터 기준 (2026-11-17 파란 Sage 로 바뀜) → 다음에 다시 캡처
