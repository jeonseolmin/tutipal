# TUTIPAL 인수인계 (2026-11-16)

다음 세션은 이 파일 → `README.md` → 필요한 코드만 읽고 이어서 작업한다. 사용자에게는 한국어로 답한다.

## 한 줄 요약
TUTIPAL = AI 도우미 챗봇이 붙은 데스크톱 학습 서비스 (포트폴리오, 팀 2~4명).
**1단계 완료. 2단계 기능 구현 완료 (2026-11-16): Flyway · 세션 시간 제한 · `studysession` · STOMP 실시간 · `quiz`(이해도 질문).** **3단계(화면 요약 · 딥링크 로그인) 완료. 4단계(LiveKit 화면공유 · 도움 요청 · 그림 설명) 구현 · 실제 동작 확인 완료 (2026-11-16).** **5단계(자료 RAG · 공지/게시판 · 복습 · 설치 파일) 완료 (2026-11-16).** 1~5단계 계획 기능 전부 구현. 남은 것은 아래 "다음 할 일". 2단계 코드 리뷰는 사용자 결정으로 뒤로 미룸 (2026-11-16).

## 문서 (먼저 볼 것)
- 설계 문서 (Claude Docs, 최신 기준): https://claude.ai/artifact/19zPFDSXRdBFWS5Hbysz56
  - PDF 사본: `docs/TUTIPAL-설계문서.pdf`
  - 문서 댓글 "화면 요약을 세션보다 먼저 할지" → **사용자 답: 세션 먼저 (원래 계획대로), 2026-11-16**
- 보안 근거: `docs/security-session-rationale.pdf` (세션 방식 · 리프레시 토큰 미사용 근거, OWASP 점검)
- 화면 목업: `docs/mockups/screens/01~32`, `docs/mockups/widgets/` (구현할 때 번호로 찾기, 디자인 기준)
- LiveKit 설정: `livekit.yaml` (UDP 7882 하나 · TCP 7881), 키는 `.env` 의 `LIVEKIT_*`
- 화면공유 흐름 PDF: 팀장 PC 에만 있음, 저장소에는 없음 (구성도에 그림 설명 경로 · 연결 경로 카드가 빠진 상태)

## 확정된 결정 (바꾸지 말 것, 바꾸려면 사용자 확인)
| 항목 | 결정 |
|---|---|
| 앱 | 학생 · 선생님 모두 **Electron 하나** (선생님 = 앱 안 선생님 모드), 별도 웹 없음. 화면 · 데스크톱 코드는 **JavaScript** (2026-11-17 팀 결정으로 TypeScript → JS, 서버 DTO 모양은 `api/types.js` JSDoc) |
| 역할 | 계정은 `USER / ADMIN`, 선생님 · 학생은 **세션마다** (세션 연 사람 = 그 세션 TEACHER) |
| 로그인 | 소셜(카카오 · 네이버 · 구글) + **세션 쿠키(Spring Session Redis)**, **리프레시 토큰 · JWT 로그인 없음** (BFF 패턴, RFC 10017) |
| JWT | LiveKit 입장권에만 (짧은 유효시간) |
| 배포 | **AWS 안 씀**, 팀장 PC 한 대 · Docker Compose **5개 (backend · ai · db · redis · livekit)**, **nginx 없음** (livekit 추가는 2026-11-16 사용자 결정) |
| 화면 제공 | Spring 이 React 빌드 결과를 직접 제공 (`SpaConfig`), Electron 은 서버 주소(`APP_BASE_URL`)를 그대로 연다 |
| 접속 | 테스트 Tailscale, 공개 Cloudflare Tunnel(HTTPS) |
| LLM | 로컬 Ollama `gemma3:12b` 기본 (`LLM_PROVIDER=ollama`), 외부 API 는 예비 (아직 501) |
| 임베딩 | **Ollama bge-m3** (1024차원, KURE-v1 의 바탕 모델, PyTorch 없이). 자료 RAG + 복습 묶기 (2026-11-16 사용자 결정) |
| 복습 기준 | 동의한 학생만, **최근 7일 안 비슷한 내용 3번 이상 또는 10분 이상 → 제안**, 같은 묶음은 하루 한 번 (2026-11-16 사용자 결정) |
| AI 조교 | **LLM 직접 답변이 메인**, 자료 RAG 는 보조(관련 있을 때만 근거 + 출처) |
| 화면공유 | LiveKit: 방 A 선생님 → 전체, 방 B 학생 → 선생님 1:1(승인 후, 최대 4) + 그림 설명(데이터 채널 → 학생 PC 투명 오버레이) |
| 이해도 질문 | 객관식 자동 채점 + 주관식 AI 채점(선생님이 수정 가능) + AI 문제 만들기 |
| 유지 기능 | 선생님 화면 전체 공유, 공지 · 게시판, 자료 업로드 + RAG |
| 이름 | TUTIPAL, 패키지 `com.tutipal`, DB `tutipal`, 딥링크 `tutipal://` |

## 지금 구현된 것
- backend (`backend/src/main/java/com/tutipal`)
  - `domain/user`: 소셜 로그인 → users 저장 (`SocialOAuth2UserService`, `SocialProfile`), `GET /api/users/me`
  - `domain/tutor`: `POST /api/tutor/chat` SSE (`TutorController`), 주제 지시문 `TopicPrompt`, 동시 제한 `AiSlots`, 15초 ping, 중지 시 GPU 자리 반납
    - 요청에 `sessionId` 가 있으면 `StudySessionService.chatSettings` → 세션 주제 + 난이도(1~5) + 정답 숨김 지시문 (`TutorService.sessionPrompt`). 권한 오류는 SSE `error` 이벤트로
  - `domain/studysession` (V2 마이그레이션): `/api/sessions` 개설(TEACHER, 6자리 코드 `InviteCodes`) · `/join`(STUDENT, 소문자·하이픈 허용) · 내 세션 목록 · 조회 · `/participants`(선생님) · `PUT /settings`(선생님) · `/end`(선생님, 이후 참여·챗봇 410)
    - 참가자가 아니면 404(존재 숨김), 학생이 선생님 기능이면 403. `preferMaterials` 는 저장만 (RAG 5단계)
    - ponytail: 초대 코드 맞히기 시도 제한 없음 → 공개 운영 전에 추가
  - `global/ai/AiClient`: FastAPI 호출 한 곳 (HTTP/1.1 고정, 내부 키). `chatStream`(챗봇) + `complete(..., jsonMode)`(채점 · 문제 만들기)
  - `global/ai/AiSlots`: GPU 동시 호출 제한을 챗봇 · 채점 · 문제 만들기가 같이 씀 (기본 2개, 채점은 최대 5분 대기)
  - `global/realtime`: `SessionEvents`(알림 = type+id 만, 커밋 뒤 전송 `AfterCommit`) · `Presence`(구독 = 접속 중) · `StompAuthInterceptor`(구독 권한, SEND 전부 차단)
    - 토픽: `/topic/sessions/{id}/events`(참가자) · `/topic/sessions/{id}/teacher`(선생님) · `/user/queue/events`(본인)
    - 화면은 알림을 받으면 REST 로 다시 읽음 (권한 검사는 REST 한 곳)
  - `domain/auth`: 앱 딥링크 로그인. `GET /auth/app/start?provider=&challenge=` (브라우저) → 소셜 성공 시 `AppLoginSuccessHandler` 가 1회용 코드(Redis 60초) 발급 + 브라우저 세션 버림 → `/auth/app/done?code=`(React) → `tutipal://auth?code=` → 앱이 `POST /api/auth/app/exchange {code, verifier}` (permitAll · CSRF 제외, PKCE 식 verifier 검사, 코드는 한 번 쓰면 사라짐) → 앱 세션 쿠키
    - `SessionLogin`: 세션 로그인 만들기 공통 (개발용 로그인도 사용). `provider=dev` 는 DEV_LOGIN=true 일 때만 (딥링크 흐름 시험용)
  - `domain/screensummary`: `POST /api/screen {image(base64 JPEG/PNG), question?}` → `{text}`. gemma3:12b 비전 (따뜻하면 2~3초). 이미지 · 결과 저장 안 함, JPEG/PNG 머리 바이트 검사, 자동 요약은 GPU 자리 5초만 기다리고 503 이면 위젯이 다음 차례에
  - `domain/screenshare` (V4 help_requests): LiveKit 입장권 `LiveKitTokens`(HS256 JWT, 표준 라이브러리, 10분, 화면 트랙만 허용) · 방 A `session-{id}-class`(선생님만 화면 올림) · 도움 요청 대기→수락(동시 최대 4)/나중에/종료 · 방 B `session-{id}-help-{요청id}`(학생 화면만 올림, 선생님 보기 + 그림 데이터만)
    - API: `POST /api/sessions/{id}/screenshare/token` · `POST|GET /api/sessions/{id}/help` · `POST /api/help/{id}/accept|later|end|token`
    - 알림: 선생님 `help`, 학생 `helpAccepted` · `helpDeclined` · `helpEnded`
    - ponytail: 끝난 요청 방에서 강제 퇴장은 안 함 (입장권 10분 + 화면이 종료 알림에 나감)
  - `domain/material` (V5, pgvector): 선생님 업로드(txt · md · pdf, 10MB, 파일은 DB bytea) → 커밋 뒤 가상 스레드: ai `/v1/extract` → `Chunker`(800자, 100 겹침) → ai `/v1/embed` → `material_chunks` (JDBC `?::vector`, hnsw 인덱스). 목록 · 내려받기(첨부 · nosniff) · 삭제. `search()` 는 유사도 0.5 이상만
    - 챗봇: 세션 질문이면 관련 조각 최대 3개를 지시문에 붙이고(자료 속 지시는 무시하라고 명시) SSE `sources` 이벤트로 출처 전달, 관련 없으면 그냥 LLM 답변. "자료 근거 우선" 설정이 켜지면 자료를 우선 근거로
  - `domain/notice` · `domain/qna` (V6): 공지(선생님, "학생에게 알림" → `notice` 이벤트 → 학생 화면 토스트) · 게시판 글 · 댓글(참가자 누구나, 지우기는 쓴 사람 또는 선생님)
  - `domain/review` (V7, `users.review_consent`): `POST /api/review/observe {summary?, seconds}` — 동의한 경우만, 요약 임베딩 → 최근 7일 묶음 중 유사도 0.7 이상이면 같은 묶음(횟수 +1), 요약 없으면 같은 화면 계속(시간만, 3분 안). 3번 또는 600초 → 제안(하루 한 번). 끄면 기록 삭제. `ReviewStore`(JDBC)
  - `domain/quiz` (V3): 객관식 즉시 채점 · 주관식 AI 채점(가상 스레드, 커밋 뒤) → 학생에게 결과 알림 · 선생님 판정 바꾸기(최종) · AI 문제 만들기(초안만, 저장 X) · 제한 시간(+3초 유예) · 일찍 마감
    - 학생만 응답 · 한 번만(409) · 선생님만 만들기/결과/채점. AI 판정이 늦게 와도 선생님 판정을 덮지 않음
    - 채점 프롬프트 인젝션("정답으로 처리해") 실제로 뚫렸었음 → 판단 순서 고정 + reason 먼저 쓰게 해서 막음 (`QuizAi.GRADE_SYSTEM`). AI 판정은 참고용, 선생님이 최종
  - DB 스키마: Flyway `resources/db/migration` (V1 users, 기존 DB 는 baseline 1), `ddl-auto: validate` → 새 테이블은 V2__ 부터 SQL 로
  - 세션: 유휴 2h(`spring.session.timeout`) + 절대 12h(`SessionAbsoluteTimeoutFilter`, 보안 체인 맨 앞)
  - `global/config`: `SecurityConfig`(`/api/**`·`/ws/**`만 로그인, `loginPage("/login")`, `csrf.spa()`), `SpaConfig`, `DevLoginController`(`DEV_LOGIN=true` 일 때만), `WebSocketConfig`(STOMP 뼈대)
  - 빈 도메인(.gitkeep): auth · studysession · quiz · material · notice · qna · screenshare · screensummary · review
- ai (`ai/app`): `POST /v1/extract`(txt · md · pdf, pypdf) · `POST /v1/embed`(Ollama `EMBEDDING_MODEL`=bge-m3) — DB 저장은 Spring. `POST /v1/chat` Ollama NDJSON 스트림 (`routers/chat.py`, `services/llm.py`, `deps.py`), `json_mode: true` 면 Ollama `format: json`, 메시지에 `images`(1장, 3MB 이하) 넣으면 비전
- frontend (`frontend/src`): 디자인 토큰(`styles/`), SVG 캐릭터 `components/Mascot.jsx`(표정 8개), `AppShell`, 페이지 Login · Home · Chat · Widget, `api/`(types · client · tutor · sessions)
  - 세션 화면: `CreateSessionPage`(11) · `JoinSessionPage`(12) · `SessionPage`(역할별: 학생 13 = 라이브 자리 + 세션 챗봇 / 선생님 20·21 = 참가자 · 주제 설정 · 챗봇 미리보기 · 종료), 홈에 참여 중 세션 목록
  - `ChatPanel`(ChatPage 에서 분리)을 개인 · 세션 챗봇이 같이 씀
  - 실시간: `api/realtime.ts` `useSessionEvents` (@stomp/stompjs 7.5.0, 3초 재연결, 재연결 시 전체 다시 읽기), 머리말에 "실시간" 표시
  - 이해도 질문: `pages/QuizPanels.jsx` — 선생님 탭(22 만들기 + AI 초안, 23 정답률 막대 · 응답 수 · 남은 시간, 24 응답 검토 · 맞음/틀림) / 학생 팝업(15 응답 + 타이머 → 16 결과, 주관식은 채점 중 → 실시간 결과)
  - 선생님 참가자 목록: 접속 중 초록 점, 참여 · 접속 변화 자동 갱신
  - 4단계 화면: `pages/LivePanels.jsx` — 선생님 "화면 공유" 탭(27, 창 고르기 · 시청 수) · "도움 요청" 탭(25, 대기 시간 · 수락/나중에 · 새 요청 빨간 점) → 학생 화면 보기(26, 펜 · 형광펜 · 지우개 · 3색 · 모두 지우기) / 학생 "라이브" 탭(13) · 머리말 "✋ 도움 요청"(17) · 진행 중 칩. `pages/OverlayPage.jsx`(/overlay, 19). 그림 규칙 `draw.js`(0~1 비율 좌표, 받은 메시지 검사) · livekit-client 2.23.0
  - 5단계 화면: `pages/BoardPanels.jsx` — `MaterialsPanel`(28, 끌어다 놓기 업로드 · 처리 중/완료/실패) · `NoticesPanel`(29 왼쪽) · `BoardPanel`(29 오른쪽, 글 · 댓글). 선생님 "자료 · 공지" · "게시판" 탭, 학생 공지 · 게시판 · 자료 탭, 새 공지 토스트(31). `ReviewPage`(10, /review, 동의 스위치 · N번 봄/N분 봄 · 설명 듣기 · 기록 삭제), 위젯 복습 제안 말풍선(09) → 메인 창 `/review?explain=` → `/chat?ask=` 자동 질문. 챗봇 답변 아래 "참고한 수업 자료"
  - 3단계 화면: `LoginPage`(앱이면 소셜 버튼 → 브라우저 + 대기 화면 02) · `AppLoginDonePage`(/auth/app/done) · `ScreenConsentPage`(04, /screen-consent, 홈 · 위젯 · 트레이에서 열림) · `WidgetPage`(07: 40초마다 캡처, 바뀐 경우만 요약 / 08: 질문하기 → 지금 화면에 대해 질문, 꺼져 있으면 일반 챗봇)
  - 개발용 로그인 `POST /dev/login?n=1~9` → 계정 여러 개. 화면에서는 시크릿 창으로 `http://localhost/login?dev=2` → "개발용 로그인"
- desktop (`desktop/src`): 메인 창 + 투명 캐릭터 위젯 창 + 트레이, 이동 제한(우리 서버만, 소셜 로그인 페이지도 앱 안에서 안 엶) · 권한 거절
  - `main/applogin.ts`: 브라우저 로그인 시작(verifier 메모리 5분) · 딥링크 교환(`session.defaultSession.fetch`). `index.ts` 에서 `tutipal://` 등록(HKCU, 개발 모드는 electron.exe+경로) · `second-instance`(Windows) · `open-url`(mac)
  - `main/screen.ts`: 창 목록(`desktopCapturer`, 우리 창 제외) · 설정 `userData/screen.json` · 고른 창 캡처(JPEG 1280x800, 해시로 변화 감지, 이름으로 다시 찾기)
  - `main/share.ts`: `setDisplayMediaRequestHandler`(우리 페이지만, 고른 창 또는 주 모니터) · 그림 오버레이 창(주 모니터 전체, 투명 · 클릭 통과 · 항상 위 · `setContentProtection` 으로 공유 화면에 안 찍힘)
  - 권한: `display-capture` 만 우리 페이지에서 허용, 나머지(카메라 · 마이크 등) 거절
  - preload: `openChat` · `toggleWidget` · `loginInBrowser` · `openScreenConsent` · `screenSources` · `get/setScreenSettings` · `captureScreen` · `onScreenSettingsChanged` · `setShareSource` · `showOverlay` · `sendOverlayDraw` · `onOverlayDraw`

## 실행 · 검증 명령
```sh
cd C:/PROJECTS/TUTIPAL
docker compose up -d --build                 # http://localhost (Docker Desktop 먼저 켜기)
cd desktop && npm run dev                    # 앱 (개발 모드)
cd desktop && npm run dist                   # 설치 파일 release/TUTIPAL Setup 0.0.1.exe (desktop/README.md)
cd backend && ./gradlew test                 # 71개 통과 (2026-11-17)
cd ai && uv run --no-project --python 3.12 --with-requirements requirements.txt --with-requirements requirements-dev.txt pytest -q   # 15개 통과

```
- 로그인: 앱/브라우저에서 "개발용 로그인 (내 PC 전용)" (`.env` 의 `DEV_LOGIN=true`). 소셜 키는 아직 더미값
- 첫 챗봇 질문은 모델 로딩으로 30~50초, 이후 약 7초

## 함정 (이번 세션에서 실제로 겪은 것)
- Windows Git Bash 의 `curl -d '한글'` 은 UTF-8 이 아님 → 400. 본문은 UTF-8 파일로 `--data-binary @파일`
- JDK HttpClient 기본 HTTP/2 업그레이드를 uvicorn 이 거부 → `AiClient` 는 HTTP/1.1 고정 (되돌리지 말 것)
- `ddl-auto: update` 는 enum CHECK 제약을 안 바꿈 → Flyway 로 교체 완료. 이제 엔티티만 바꾸면 기동 실패(validate), 마이그레이션 SQL 을 같이 추가할 것
- 탐색기 창이 프로젝트 폴더를 잡고 있으면 폴더 이동/이름 변경 실패
- 사용자가 앱을 직접 쓰는 중일 수 있으니 Electron 앱을 마우스 자동 클릭으로 테스트하지 말 것 (크롬 테스트 탭은 사용자 허락 받고 사용함)
- 크롬 한 개로 선생님 · 학생 동시 시험: 선생님 `http://localhost`, 학생 `http://127.0.0.1/login?dev=2` (주소가 달라 쿠키가 따로)
- 이 셸(Git Bash)에서 heredoc 안에 따옴표 · 백틱이 섞이면 가끔 파싱 실패 → 긴 파일은 Write 도구로
- 생성자가 두 개인 Spring 빈은 `@Autowired` 를 붙여야 기동됨 (QuizService 에서 겪음)
- 크롬 창이 최소화돼 있으면 확장 스크린샷이 타임아웃 → `find` / `read_page` 로 확인
- Windows 콘솔(cp949)에 한글 JSON 을 찍으면 깨짐 → `PYTHONIOENCODING=utf-8`
- 개발용 계정(dev-1)에 테스트로 만든 세션이 여러 개 남아 있음 (홈 목록이 김). 필요하면 각 세션에서 종료
- GitHub 공개 저장소 https://github.com/jeonseolmin/tutipal (2026-11-17). `main` 보호(리뷰 1명 승인 PR 만), 협업 규칙은 `CONTRIBUTING.md`. 커밋 끝에 `Co-Authored-By` 줄
- 이 PC: RTX 3060 12GB, 32GB RAM, C: 여유 약 57GB, Windows 10 Home, Ollama 에 gemma3:12b · llama3.2

## 다음 할 일 (2단계, 세션 먼저로 확정)
1. ~~Flyway 도입, 세션 유휴 2시간 · 절대 12시간 제한~~ ✅ (이전 8h 세션은 만료될 때까지 남음)
2. ~~`studysession`: 세션 개설(초대 코드) · 참여 · 참가자 역할 · 주제 설정 → 학생 챗봇에 적용~~ ✅ (curl 로 선생님·학생·외부인 3계정 시나리오 확인, 화면은 사용자 확인 필요)
3. ~~STOMP 실시간 (세션 쿠키 인증), `quiz`: 객관식/주관식 · AI 채점 · 실시간 정답률~~ ✅ (Python STOMP 점검 15항목 + 크롬 두 탭으로 선생님 · 학생 흐름 확인)
4. ~~화면: 목업 11~13 · 15 · 16 · 20~24~~ ✅ (선생님 모드는 목업의 왼쪽 메뉴 대신 위쪽 탭)

## 다음 할 일 (5단계까지 끝난 뒤) — 2026-11-17 부터는 GitHub 이슈가 기준
- 이슈 #1 마크다운 표시 · #2 초대 코드 시도 제한 · #3 AI 호출 사용자별 제한 · #4 소셜 키 + 실제 로그인 · #5 공개 시연 접속(Tunnel · LiveKit) · #6 GitHub Actions · #7 설치 파일 서명 · #8 위젯 복습 제안 확인 · #9 유사도 기준 재조정 · #10 도움 방 강제 퇴장 · #11 선생님 메뉴 목업처럼
- 라벨: backend · frontend · desktop · ai · infra · security (+ enhancement · bug)

### (이전 메모)
1. ~~미뤄 둔 코드 리뷰~~ ✅ 2026-11-17 (아래 "코드 리뷰 반영"). **서버 재배포 필요: `docker compose up -d --build`** (리뷰 반영 때 Docker Desktop 이 꺼져 있었음, V8 마이그레이션 포함)
2. 챗봇 답변 마크다운 표시 (`**` 가 그대로 보임, 1단계부터)
3. 소셜 키 발급 후 실제 카카오 · 네이버 · 구글 앱 로그인 확인, 설치 파일 코드 서명
4. 공개 운영 전: 초대 코드 맞히기 · 화면 요약 호출에 사용자별 횟수 제한, LiveKit 공개 접속(Tailscale IP / 포트 / TURN)
5. 위젯 복습 제안 말풍선 실제 흐름 확인, 복습 유사도 0.7 · RAG 0.5 를 실제 데이터로 다시 맞추기
5. 각 단계 완료 기준: 테스트 통과 + 실제 동작 확인 + 별도 코드 리뷰(`/code-review` high) 반영
- 이후: 3단계 화면 요약 · 딥링크 로그인 → 4단계 LiveKit → 5단계 RAG · 공지 · 복습 · 설치 파일

## 코드 리뷰 반영 (2026-11-17, `/code-review high` 를 폴더 전체에 — git 이 아니라 diff 대신 전체 코드)
지적 10건 + 작은 것 3건을 코드로 하나씩 확인한 뒤 전부 반영:
- AI 서버 실패(`AiUnavailableException`)가 빈 500 으로 나감 → 전역 처리기에서 503 + 문구 (위젯은 503 이면 조용히 다음 차례)
- 새 이해도 질문이 오면 이전 질문의 선택 · 입력이 남음 → 학생 질문 창에 `key={quiz.id}`
- 자료 조각 3개 + 긴 파일명이면 지시문이 FastAPI 한도(4000)를 넘어 챗봇 전체 실패 → 한도 12000, 조각 1500자 · 출처 120자로 자름
- 공용 HNSW 인덱스는 "가까운 순 → 세션 거르기" 라 다른 세션 자료가 많으면 결과가 빠짐 → V8 에서 인덱스 삭제 (세션 인덱스로 거른 뒤 정확 계산)
- 복습 "최근 7일 안" 이 지켜지지 않음 (횟수가 계속 누적) → 묶음의 7일 창이 지나면 새로 셈 (`ReviewStore.touch` windowStart, 근사)
- 위젯이 직전 화면을 본 시간을 새 화면 묶음에 더함 → 바뀌면 먼저 지난 묶음에 시간, 새 요약은 0초
- 선생님 "나중에" 를 학생이 알 수 없음 → "선생님이 조금 뒤에 볼게요" 안내
- 초대 코드 정규식이 `"[\\s-]"` 가 아니라 `"[\s-]"`(Java 에서는 공백 한 글자)로 들어가 탭 · 줄바꿈이 안 지워짐 (셸 heredoc 이 역슬래시를 먹음) → 고침 + 회귀 테스트. 다른 Java 정규식은 문제없음 확인
- 화면 분석이 창 id 를 못 찾으면 **이름이 같은 다른 창**을 찍음 (동의 범위 밖) → id 로만, 못 찾으면 "창 다시 고르기" 버튼
- 같은 `vector()` 가 두 곳 → `AiClient.pgVector` 하나로
- 작은 것: "나중에" 는 대기 중 요청만(진행 중은 409), 학생 수는 count 쿼리, 자료 유무는 EXISTS
- 남은 확인: 재배포 후 RAG · 복습 · 위젯 "창 다시 고르기" 실제 동작

## 5단계 확인 결과 (2026-11-16)
- RAG: 자료(md) 업로드 → 처리 완료(조각 1) → 학생 "이번 주 과제 마감?" → "금요일 오후 6시, 학번_pyramid.java" + 출처 `3주차-반복문.md` ✅ / 관계없는 질문(파이썬 정렬) → 출처 없음 ✅ / 학생 업로드 403 · exe 415 · 외부인 404 · 내려받기 원본과 같음 ✅
- 공지 · 게시판: 학생 탭에 공지 · 글 · 댓글 · 자료(완료) 보임, 학생은 업로드 칸 없음 ✅ / 선생님이 알림 공지 → 학생 화면 토스트 "새 공지: 내일은 퀴즈가 있어요" ✅
- 복습: 동의 끔 → 기록 안 함 ✅ / 같은 화면 요약 3개 → 한 묶음 3번 → 3번째에 제안 ✅ / 다른 화면 따로 ✅ / 같은 날 두 번째 제안 없음 ✅ / 끄면 삭제 ✅ / 복습 기록 화면 → 설명 듣기 → 챗봇이 자동 질문 · 답변 ✅
- 설치 파일: `npm run dist` → 약 120MB NSIS exe, fuse 적용 확인(RunAsNode 끔 · 쿠키 암호화 · inspect 끔 · asar 검사), 압축 푼 앱 실행 → 서버 로그인 화면 · 위젯 ✅. 실제 설치는 안 해 봄 (사용자 PC 프로그램 목록에 남아서)
- 확인 못 한 것: 위젯 복습 제안 말풍선(09)이 실제 화면 분석 흐름에서 뜨는지 (서버 · 기록 화면은 확인, 위젯은 화면이 3번 바뀌어야 해서 미확인)
- 고친 것: ① `@Modifying` 업데이트를 트랜잭션 밖 스레드에서 불러 실패 + 실패 처리도 같은 이유로 실패 → 자료가 "처리 중" 에 영원히 멈춤 → 저장소 메서드에 `@Transactional` ② 복습 묶음 유사도 0.8 은 같은 화면도 갈라짐 → 실측 후 0.7 ③ 레코드 필드 `notify` 는 Object.notify 와 겹쳐 컴파일 안 됨 → `alert` ④ 사라질 API `PAYLOAD_TOO_LARGE` → `CONTENT_TOO_LARGE` (빌드에 `-Xlint:deprecation` 추가)
- 테스트 중 설치 파일 앱을 켜서 `tutipal://` 가 그 exe 로 바뀌었던 것 → 개발 앱을 다시 켜서 원래대로 돌려 둠

## 4단계 확인 결과 (2026-11-16, Electron CDP + 크롬)
- 수업 공유: Electron 선생님이 메모장 창 공유 → 크롬 학생(127.0.0.1) "라이브" 탭에 LIVE 영상 ✅, 선생님 "시청 1명" ✅
- 도움 요청: Electron 학생 요청(17) → 크롬 선생님 목록 "대기 0분"(25) → 수락 → 학생 "선생님이 보는 중" + 주 모니터 공유 + 오버레이 창 ✅ → 선생님 화면 보기(960x540 수신) → 펜 동그라미 · 형광펜 → 학생 오버레이에 그대로(19) ✅ → 지우개로 동그라미만 지움 ✅ → 종료 → 오버레이 닫힘 · 학생 버튼 원래대로 ✅
- 오버레이가 공유 화면에 찍히지 않음 (선생님 영상에서 동그라미 자리 빨간 점 0/80) ✅
- 실제 LiveKit 이 우리 입장권 수락 200 · 변조 401 ✅ / 외부인 404 · 중복 요청 409 · 수락 전 입장권 409 · 학생이 수락 403 · 종료 후 입장권 409 ✅
- 고친 것: ① 두 생성자 빈 `@Autowired` (LiveKitTokens, 또 겪음) ② 권한 처리기가 화면 캡처까지 거절 → `display-capture` 만 우리 페이지에 허용 ③ 오버레이가 작업 영역(1040)으로 줄어 그림이 어긋남 → 만든 뒤 `setBounds`(1080) ④ 포인터 캡처 실패 대비
- 크롬 한 개로 선생님 · 학생: localhost / 127.0.0.1 (쿠키 따로). 학생 화면 공유는 이 PC 화면이 로컬 LiveKit 으로만 돌아옴 (영상 확인은 숫자로만, 스크린샷 안 찍음)
- 공개 시연 때: Cloudflare Tunnel 은 영상(UDP)을 못 지나감 → `LIVEKIT_PUBLIC_URL` · `LIVEKIT_NODE_IP` 를 Tailscale IP 로, 또는 공유기에서 7881/tcp · 7882/udp 열기 / TURN

## 3단계 Electron 확인 결과 (2026-11-16, CDP 로 직접 실행 · 조작, 마우스 안 씀)
- 딥링크: 로그인 화면 "브라우저 경유" → 대기 화면(02) → (브라우저 대신 curl) → Windows `Start-Process tutipal://auth?code=` → 앱이 교환 후 홈, `/api/users/me` 200 ✅ / 같은 링크 다시 열기 → 무시 ✅
  - 실제 브라우저의 "TUTIPAL 열기?" 대화상자와 실제 소셜(구글) 로그인은 사람이 해야 함 (소셜 키 발급 후)
- 화면 분석: 동의 화면(04)에서 메모장 창 고르기 → 위젯 요약(07) "자바 for문 … 1부터 10까지 더하는…" ✅ / 질문하기(08) "sum 최종 값?" → "55, i<10 이면 45" ✅ / 일시정지 50초 동안 캡처 0회 ✅ / 끄기 후 45초 캡처 0회 ✅ / 우리 창은 목록에서 빠짐 ✅
- 고친 것: ① 개발 모드 프로토콜 등록이 `argv[1]` 이라 `electron --inspect .` 처럼 옵션이 앞에 오면 옵션이 앱 경로로 등록됨 → `app.getAppPath()` ② 동의 화면 이모지(🪟)가 Windows 글꼴에서 네모로 보임 → SVG 아이콘 ③ 숨긴 라디오(absolute)가 카드 밖으로 새서 페이지가 스크롤됨 → `.source-card { position: relative }` ④ 카드가 창 높이를 넘음 → 크기 줄임
- 확인 못 한 것: 첫 실행 때 사용자가 고른 Slack 창 요약이 "살펴보는 중…" 에서 안 바뀌었음 (개인 창이라 다시 캡처하지 않음, 원인 미확인 — 최소화된 창이면 `gone` 이어야 함). 다음에 일반 창으로 재현해 볼 것
- 테스트 방법: `npx electron --inspect=9229 --remote-debugging-port=9223 .` → `desktop/scripts/cdp.py` (eval/shot, `uv run --with websockets python desktop/scripts/cdp.py targets`). 끝나면 반드시 앱 종료 (디버그 포트가 열려 있으면 이 PC 의 다른 프로그램이 앱을 조종할 수 있음)
- 이 PC 상태: `HKCU\Software\Classes\tutipal` 등록됨 (electron.exe + desktop 폴더), 화면 분석은 꺼 둠 (userData/screen.json enabled=false)

## 남은 결정 · 확인
- 서브도메인 · 소셜 키 발급 (이슈 #4 · #5). 복습 기준 · 임베딩은 2026-11-16 확정, git 은 2026-11-17 시작
