// Electron preload 가 공개하는 기능 (desktop/src/preload). 웹 브라우저에서는 undefined.
interface ScreenSettings {
  enabled: boolean
  sourceId: string
  sourceName: string
}

interface ScreenSource {
  id: string
  name: string
  /** data:image/png;base64,… 작은 미리보기 */
  thumbnail: string
}

type ScreenCapture =
  | { status: 'off' }
  | { status: 'gone' }
  | { status: 'ok'; image: string; changed: boolean }

interface DesktopBridge {
  isDesktop: true
  openChat(topic?: string): void
  toggleWidget(): void
  loginInBrowser(provider: 'google' | 'naver' | 'kakao' | 'dev'): void
  openScreenConsent(): void
  /** 복습 "설명 듣기": 메인 창에서 그 기록을 챗봇으로 */
  openReview(id: number): void
  screenSources(): Promise<ScreenSource[]>
  getScreenSettings(): Promise<ScreenSettings>
  setScreenSettings(s: ScreenSettings): Promise<ScreenSettings>
  captureScreen(): Promise<ScreenCapture>
  /** 다음 getDisplayMedia 에서 공유할 창/화면 (빈 값 = 주 모니터) */
  setShareSource(id: string): void
  /** 학생 PC 그림 오버레이(목업 19) */
  showOverlay(on: boolean): void
  sendOverlayDraw(message: string): void
  onOverlayDraw(cb: (message: string) => void): () => void
  /** 설정이 바뀌면 호출, 돌려받은 함수로 해제 */
  onScreenSettingsChanged(cb: () => void): () => void
}

interface Window {
  desktop?: DesktopBridge
}
