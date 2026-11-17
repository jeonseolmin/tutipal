# 함께 작업하는 방법

처음이면 `README.md`(구조 · 실행) → `HANDOFF.md`(지금 상태 · 확정된 결정 · 함정) 순서로 읽는다.
확정된 결정(HANDOFF "확정된 결정" 표)을 바꾸려면 팀에서 먼저 정하고 표를 고친다.

## 일 고르기
- 할 일은 GitHub 이슈에 있다. 영역 라벨: `backend` · `frontend` · `desktop` · `ai` · `infra`, 종류 라벨: `bug` · `enhancement` · `security`
- 맡을 이슈에 자기를 Assignee 로 지정하고 시작한다 (겹치지 않게)

## 브랜치 · PR
- `main` 에는 직접 push 하지 않는다 (보호됨). 이슈마다 브랜치: `feat/12-markdown-chat`, `fix/15-invite-limit` (`종류/이슈번호-짧은설명`)
- PR 본문에 `Closes #12`, 무엇을 · 왜 · 어떻게 확인했는지 적는다
- 리뷰 1명 승인 후 합친다 (Squash merge)

## 합치기 전에 확인
```sh
cd backend && ./gradlew test
cd ai && uv run --no-project --python 3.12 --with-requirements requirements.txt --with-requirements requirements-dev.txt pytest -q
cd frontend && npm run build        # Vite 빌드 (문법 · import 오류)
cd desktop && npm run check        # node --check 로 문법 검사
```
- 백엔드 DTO 를 바꾸면 `frontend/src/api/types.js`(JSDoc) 도 같이, 화면에서 그 필드를 쓰는 곳도 검색해서 같이 (타입 검사가 없음)
- DB 를 바꾸면 `backend/src/main/resources/db/migration/V<다음번호>__설명.sql` 추가 (이미 합쳐진 마이그레이션은 고치지 않는다)
- 화면을 바꿨으면 실제로 띄워서 확인 (`docker compose up -d --build` → http://localhost, 개발용 로그인)

## 비밀값
- `.env` 는 올리지 않는다 (`.gitignore`). 새 설정이 생기면 `.env.example` 에 이름과 설명만 추가
- 소셜 로그인 키 · LiveKit 시크릿 · 내부 키는 팀 메신저 등 저장소 밖에서 공유
