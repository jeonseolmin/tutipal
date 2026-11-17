# TUTIPAL

AI 도우미 챗봇이 붙은 학습 서비스. 누구나 **개인 학습**을 하거나 **단체 학습 세션**을 열 수 있고,
세션을 연 사람이 그 세션에서만 선생님이 된다. 학생과 선생님 모두 같은 Electron 데스크톱 앱을 쓴다 (선생님은 앱 안의 선생님 모드, 별도 웹 없음).
서버는 팀장 PC 한 대(로컬 서버 + 로컬 LLM), AWS 는 쓰지 않는다.

그림(`docs/`): `architecture.png` 전체 · `structure.png` 디렉토리/MVC · `screen-share.png` 화면공유 ·
`desktop-options.png` · `tauri-vs-electron.png` 데스크톱 기술 선택 근거 · `app-auth.png` 로그인 (※ 세션 통일 전 그림)

## 기능

| 선생님 (세션을 연 사람) | 학생 |
|---|---|
| 단체 학습 세션 개설 (그 세션에서만 선생님 모드) | 개인 학습 시작 (주제 선택: 코딩·영어·수학 …) |
| 세션 학습 주제 설정 → 학생 챗봇에 적용 | 단체 세션 참여 (챗봇이 선생님 설정을 따름) |
| 이해도 확인 질문 전송 (객관식 자동 채점 · 주관식 AI 채점) | 현재 화면 요약 (오른쪽 아래 캐릭터 말풍선) |
| 질문별 정답률 확인 | 캐릭터를 눌러 현재 화면에 대해 질문 |
| 도움 요청한 학생 화면에 그림으로 설명 | 자주·오래 본 내용 복습 제안 (기준은 추후) |
| 내 화면을 세션 전체에 공유 | 이해도 질문 응답 · 선생님에게 도움 요청 |
| 공지 · 게시판 · 자료 업로드 | 공지 · 게시판 · 자료 보기 |

AI 조교: **LLM 이 직접 답하는 것이 기본**, 세션 자료 중 관련 있는 것이 검색되면(RAG) 근거로 덧붙이고 출처 표시.

## 결정 사항

| 항목 | 결정 | 이유 |
|---|---|---|
| 앱 | Electron + TypeScript 하나 (학생 · 선생님 모드), 별도 웹 없음 | 화면(React) 한 벌만 관리, 다른 프로그램 위 위젯·그림 오버레이는 앱만 가능 |
| nginx | 없음 — Spring 이 React 화면과 API 를 한 주소로 제공 | 모두 Electron, HTTPS 는 Cloudflare Tunnel 이 맡음 |
| 역할 | 계정은 USER/ADMIN, **선생님/학생은 세션마다** (세션 참가자 역할) | 세션을 연 사람만 그 세션의 선생님 |
| 로그인 | **세션 쿠키 (Redis) 하나로 통일**, 리프레시 토큰 없음 | 서버 1대 · 같은 주소 · 웹/Electron 모두 쿠키 사용 · 즉시 차단 |
| JWT | LiveKit 입장권에만 (짧은 유효시간) | LiveKit 은 우리 세션 장부를 못 봄 |
| 화면공유 | LiveKit (방 A: 선생님→전체, 방 B: 학생→선생님 1:1 승인 + 그림 설명) | 무료 오픈소스 SFU |
| 실시간 | STOMP (질문 전송·응답·정답률·도움 요청·공지 알림), 그림 선은 LiveKit 데이터 채널 | 기존 WebSocket 재사용 |
| LLM | 로컬 Ollama 기본 (`LLM_PROVIDER=ollama`), 외부 API 예비 | 비용 0 · 데이터 외부 유출 없음 |
| 임베딩 | Ollama bge-m3: 자료 RAG + 복습용 "비슷한 화면 요약" 묶기 | KURE-v1 의 바탕 모델, PyTorch 없이 이미 쓰는 Ollama 로 (2026-11-16 결정) |
| 배포 | 팀장 PC + Tailscale(테스트) / Cloudflare Tunnel(공개) | AWS 비용 없음 |

## 디렉토리

```
TUTIPAL/
├─ docker-compose.yml · .env.example · db/init.sql
├─ docs/                  설계 그림
├─ frontend/              React + TS + Vite — 빌드 결과를 Spring 이미지에 넣어 Spring 이 제공 (Electron 이 서버 주소로 열어서 사용)
│  └─ src/
│     ├─ main.tsx · App.tsx
│     ├─ api/             types.ts (백엔드 DTO 1:1) · client.ts (fetch · CSRF · 오류)
│     ├─ pages/           화면 단위 (홈 · 세션 · 챗봇 · 선생님 모드 · 캐릭터 위젯 …)
│     └─ components/      공통 UI
├─ desktop/               Electron + TS (예정) — desktop/README.md
│  └─ src/  main/ (창 · 캐릭터 위젯 · 그림 오버레이 · 화면 캡처 · 딥링크 · 트레이) · preload/
├─ backend/               Spring Boot 4 · Java 21
│  └─ src/main/java/com/tutipal/
│     ├─ global/          config · exception · common · ai (FastAPI 호출 한 곳)
│     └─ domain/
│        ├─ user/         ✔ 완성 예시 (소셜 로그인 → users)
│        ├─ auth/         앱 로그인: 1회용 코드(Redis 60초) → 세션 쿠키 교환
│        ├─ studysession/ 단체 세션 개설·참여·종료 · 참가자 역할(TEACHER/STUDENT) · 학습 주제 설정
│        ├─ tutor/        도우미 챗봇 (LLM 기본 + 자료 RAG 보조, 개인/세션 주제 설정 적용)
│        ├─ quiz/         이해도 확인 질문 · 응답 · 채점(객관식 자동 · 주관식 AI) · 정답률
│        ├─ material/     자료 업로드 · 임베딩 (RAG 재료)
│        ├─ notice/ · qna/ 공지 · 게시판
│        ├─ screenshare/  LiveKit 입장권(JWT) · 선생님 화면 공유 · 도움 요청/승인 · 그림 설명 권한
│        ├─ screensummary/ 현재 화면 요약 · 화면 질문 (→ global/ai → FastAPI vision)
│        └─ review/       자주·오래 본 내용 저장 · 복습 제안
└─ ai/                    FastAPI (외부 비공개)
   └─ app/
      ├─ main.py
      ├─ routers/         embed · chat · vision 엔드포인트
      ├─ services/        KURE 임베딩 · LLM(ollama|api) · 비전
      └─ schemas/         요청/응답 모델
```

빈 폴더는 `.gitkeep` 으로 자리만 잡아 둠.

### 백엔드 규칙 (도메인별 MVC)
- Controller 는 요청/응답만, 판단은 Service, 화면엔 항상 dto (엔티티 직접 노출 X)
- 다른 도메인 데이터는 그 도메인의 Service 로 (Repository 직접 호출 X)
- FastAPI 호출은 `global/ai` 의 클라이언트 하나로 (tutor · quiz · material · screensummary · review 가 같이 씀)
- 선생님 권한은 "이 세션의 TEACHER 인가" 를 Service 에서 확인 (계정 역할 아님)
- 필요한 계층만 만든다 (예: `auth` 는 Redis 만 써서 entity/repository 없음)

### 프론트 규칙
- 서버와 주고받는 코드는 `api/` 에만. 백엔드 DTO 를 바꾸면 `api/types.ts` 도 같이 수정

## 실행

```sh
cp .env.example .env          # 값 채우기 (소셜 키 없이도 기동은 됨)
docker compose up -d --build
curl http://localhost/health  # {"status":"ok","virtualThread":true}
```

브라우저에서 `http://localhost` → 카카오 · 네이버 · 구글 로그인 버튼.
5432 를 이미 쓰는 PostgreSQL 이 있으면 `.env` 의 `DB_HOST_PORT=5433` 그대로 둔다.

### 화면 개발

```sh
docker compose up -d db redis ai
cd backend && ./gradlew bootRun              # 8080 (.env 값을 환경변수로)
cd frontend && npm install && npm run dev    # http://localhost:5173, /api 등은 8080 으로 프록시
npm run typecheck                            # 타입 검사만 (build 도 먼저 검사)
cd backend && ./gradlew test
```

소셜 콘솔에 개발용 Redirect URI `http://localhost:5173/login/oauth2/code/{google|naver|kakao}` 도 등록.

## 1단계 구현 (지금 되는 것)

화면 목업: `docs/mockups/screens/01~32`, `docs/mockups/widgets/` (구현할 때 번호로 찾기)

| 화면 | 상태 |
|---|---|
| 01 로그인 | ✅ 소셜 3종 + 개발용 로그인(내 PC 에서 열 때만 버튼 보임) |
| 03 홈 | ✅ 주제 카드 → 챗봇, 세션 만들기 · 참여, 참여 중 세션 목록 |
| 06 도우미 챗봇 | ✅ 로컬 LLM(gemma3:12b) 답변이 글자 단위로 스트리밍, 주제별 지시문, 중지 버튼 |
| 07 캐릭터 위젯 | ✅ Electron 투명·항상 위 창, 고른 창 요약(40초마다 · 바뀐 경우만), 일시정지 (3단계) |
| 02 앱 로그인 대기 | ✅ 앱의 소셜 로그인은 시스템 브라우저 → `tutipal://` 딥링크로 복귀 (3단계) |
| 04 화면 분석 동의 | ✅ 고른 창만 · 이미지 저장 안 함 · 언제든 끄기 (3단계) |
| 08 화면 질문 | ✅ 위젯에서 지금 화면에 대해 질문 (3단계) |
| 11 · 12 세션 만들기 · 참여 | ✅ 6자리 초대 코드, 만든 사람 = 그 세션 선생님 (2단계) |
| 13 학생 세션 홈 | ✅ 세션 챗봇 (선생님 설정 적용), 라이브 · 공지 · 자료는 다음 단계 |
| 20 · 21 선생님 모드 | ✅ 참가자 목록(접속 중 표시, 실시간) · 주제/난이도/정답 숨김 설정 · 세션 종료 |
| 15 · 16 이해도 질문 응답 · 결과 | ✅ 선생님이 보내면 학생 화면에 바로 뜸, 타이머, 객관식 즉시 · 주관식 AI 채점 결과 실시간 |
| 22 · 23 · 24 질문 만들기 · 정답률 · AI 채점 검토 | ✅ AI 초안 만들기, 질문별 정답률 실시간, 선생님이 맞음/틀림 최종 판정 |
| 27 · 13 화면 공유 · 라이브 | ✅ 선생님이 고른 창을 세션 전체에 공유 (LiveKit 방 A), 학생 라이브 탭 (4단계) |
| 17 · 25 도움 요청 | ✅ 학생 요청 → 선생님 목록(대기 시간) → 수락(동시 최대 4) / 나중에 (4단계) |
| 26 · 19 그림 설명 | ✅ 선생님이 학생 화면 위에 펜 · 형광펜 · 지우개 → 학생 PC 투명 오버레이에 표시 (4단계) |
| 28 자료 | ✅ txt · md · pdf 업로드 → 세션 챗봇이 관련 있을 때만 근거 + 출처 (bge-m3 RAG, 5단계) |
| 29 · 31 공지 · 게시판 · 알림 | ✅ 선생님 공지(학생에게 알림 토스트), 게시판 글 · 댓글 (5단계) |
| 09 · 10 복습 | ✅ 동의한 학생만, 비슷한 내용 3번 또는 10분 → 캐릭터 제안, 복습 기록 · 설명 듣기 · 삭제 (5단계) |
| 설치 파일 | ✅ `desktop` → `npm run dist` (NSIS .exe, tutipal:// 등록, 서명 없음) (5단계) |

흐름: 화면 `api/tutor.ts` → `POST /api/tutor/chat` (SSE) → `TutorService`(주제 지시문 · 동시 2개 제한) →
`global/ai/AiClient` → FastAPI `POST /v1/chat` (NDJSON) → Ollama.

### 실행

```sh
ollama pull gemma3:12b                       # 한 번 (약 8GB)
ollama pull bge-m3                           # 한 번 (약 1.2GB, 자료 RAG · 복습 임베딩)
# .env: LLM_PROVIDER=ollama, OLLAMA_MODEL=gemma3:12b, DEV_LOGIN=true (내 PC 테스트용)
docker compose up -d --build                 # http://localhost → "개발용 로그인" (livekit 포함 5개)
cd desktop && npm install && npm run dev     # 메인 창 + 오른쪽 아래 캐릭터 위젯, 트레이 메뉴
cd ai && uv run --no-project --with-requirements requirements.txt --with-requirements requirements-dev.txt pytest -q
```

- 첫 질문은 모델을 GPU 에 올리느라 30~50초, 이후는 빠름
- 앱의 소셜 로그인은 전부 시스템 브라우저 + `tutipal://` 딥링크 (구글이 앱 안 로그인을 막아서 통일). 1회용 코드 60초 + 앱만 아는 verifier
- DB 스키마는 Flyway(`backend/src/main/resources/db/migration`)가 만든다. 테이블을 바꾸면 `V<n>__설명.sql` 추가 (`ddl-auto: validate`)
- Windows Git Bash 에서 `curl -d '한글'` 은 UTF-8 이 아니라 400 이 남 → `--data-binary @파일` 사용

## 소셜 로그인 Redirect URI

각 콘솔에 `{APP_BASE_URL}/login/oauth2/code/{google|naver|kakao}` 등록.

- Google: Cloud Console → 사용자 인증 정보 → OAuth 클라이언트 ID (웹)
- Naver: 개발자센터 → 애플리케이션 등록 → 네이버 로그인, 서비스 URL = APP_BASE_URL
- Kakao: 개발자센터 → 카카오 로그인 활성화, 보안 → Client Secret 사용, 동의항목 닉네임·이메일

## 로컬 서버 배포 (팀장 PC)

1. 테스트: 팀원·테스트 학생이 **Tailscale** 에 로그인 → 사설망으로 접속 (포트 개방 없음)
2. 공개 시연: **Cloudflare Tunnel** 로 서브도메인 HTTPS 연결 (집 IP·포트 숨김)
   - 화면공유(LiveKit) 영상은 터널을 못 지나가므로 공유기 UDP 포트 또는 TURN 설정 필요
3. `.env`: `APP_BASE_URL=https://<서브도메인>`, `LLM_PROVIDER=ollama`, `OLLAMA_BASE_URL=http://host.docker.internal:11434`
4. GPU 는 LLM 에 양보: 임베딩(KURE)은 CPU, 같은 PC 의 다른 GPU 프로그램은 테스트 중 끄기

## 보안 메모

- 세션 쿠키: HttpOnly · SameSite · CSRF(`X-XSRF-TOKEN`) · HTTPS 에서 Secure
- DB · Redis 포트는 127.0.0.1 에만 (compose 에서 이미 제한)
- 권한 검사: "이 세션 참가자인가 / 이 세션의 선생님인가" 를 Service 에서 (가장 흔한 구멍)
- 학생 화면 이미지는 저장하지 않음, 요약 텍스트와 본 시간만 (복습 기능은 동의 후, 기록 보기·삭제 제공)

## 가상 스레드 + Redis 메모

- `spring.threads.virtual.enabled=true` → 요청 · `@Async` · 스케줄러가 가상 스레드.
- Redis 클라이언트는 Lettuce(기본, Netty 기반)라 가상 스레드와 문제없음. Jedis 로 바꾸지 말 것.
- Java 21: `synchronized` 안에서 오래 블로킹하면 캐리어 스레드 고정(pinning) → `ReentrantLock` 사용.
