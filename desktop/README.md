# desktop — Electron 앱 (TypeScript) · 학생 + 선생님 모드

화면은 따로 만들지 않고 서버 주소(`APP_BASE_URL`)의 React 화면(`frontend/`)을 그대로 연다.
같은 주소라서 웹과 똑같은 **세션 쿠키**로 로그인한다 (리프레시 토큰 없음).

```
desktop/src
├─ main/      메인 프로세스
│             · 메인 창 (서버 주소의 React 화면)
│             · 캐릭터 위젯 창: 오른쪽 아래, 항상 위, 투명 배경, 말풍선(화면 요약) · 클릭하면 화면 질문
│             · 그림 오버레이 창: 투명 전체 화면, 클릭은 아래로 통과, 선생님이 그린 선 표시
│             · 화면 캡처(desktopCapturer) · 딥링크(tutipal://) · 트레이
└─ preload/   화면에 공개할 기능만 골라서 전달 (contextBridge)
```

## 로그인 흐름 (목업 02)
1. 앱이 비밀값(verifier)을 만들고, 시스템 브라우저로 `/auth/app/start?provider=…&challenge=sha256(verifier)` 를 연다
2. 소셜 로그인 성공 → 서버가 1회용 코드(Redis, 60초)를 만들어 `/auth/app/done?code=…` → `tutipal://auth?code=…` 로 앱 복귀 (브라우저 쪽 로그인은 버림)
3. 앱 메인 프로세스 → `POST /api/auth/app/exchange {code, verifier}` → 세션 쿠키 발급 (backend `domain/auth`, 코드는 한 번만, verifier 가 맞아야 함)
4. 이후는 웹과 동일 (쿠키 + CSRF + STOMP)

## 실행 · 설치 파일
```sh
npm run dev                       # 개발: 서버 주소는 APP_BASE_URL (기본 http://localhost)
npm run dist                      # 설치 파일: release/TUTIPAL Setup x.y.z.exe (약 120MB, 서명 없음)
npm run dist -- -c.extraMetadata.tutipal.serverUrl=https://class.example.com   # 접속할 서버를 넣어서 빌드
```
- 설치 파일은 package.json `tutipal.serverUrl` 의 서버로 접속하고, `tutipal://` 를 이 앱으로 등록한다
- Electron fuse: RunAsNode 끔 · 쿠키 암호화 켬 · 명령줄 inspect 끔 · asar 무결성 검사 켬 (`build.electronFuses`)
- 코드 서명 인증서가 없어서 Windows 가 "알 수 없는 게시자" 경고를 띄움 (배포 전 서명 필요)
- 개발 중 테스트: `npx electron --inspect=9229 --remote-debugging-port=9223 .` → CDP 로 조작 (끝나면 꼭 종료)

## 지킬 것
- `contextIsolation`·`sandbox` 켜기, Node 기능은 preload 로만
- 서버 주소 외 페이지 이동·새 창 막기
- 딥링크로 들어온 코드 값 검증
- Electron fuse `EnableCookieEncryption` 켜기 (쿠키 파일 OS 암호화) — 설치 파일에 적용됨
- 화면 요약 · 복습: 사용자가 켤 때만, 고른 창만 캡처, 이미지는 저장하지 않음 (요약 텍스트·본 시간만)
- 그림 오버레이: 도움 요청을 선생님이 수락한 세션 동안만 표시, 학생이 언제든 끌 수 있게
