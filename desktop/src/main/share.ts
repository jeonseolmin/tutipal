import { BrowserWindow, desktopCapturer, screen, session } from 'electron'

/**
 * 화면공유(LiveKit) 를 위한 Electron 쪽 준비.
 * 1) 화면이 getDisplayMedia() 를 부르면, 미리 고른 창/화면을 허용한다 (Electron 은 기본 선택 창이 없음).
 *    도움 요청(학생 → 선생님)은 그림 위치가 맞도록 주 모니터 전체를 공유한다.
 * 2) 학생 PC 위의 투명 오버레이 창(목업 19)에 선생님 그림을 그린다. 클릭은 아래 프로그램으로 통과.
 */
let chosenSource = ''
let overlay: BrowserWindow | null = null

/** 화면이 다음 getDisplayMedia 에서 공유할 창/화면 id (빈 값 = 주 모니터) */
export function chooseShareSource(id: unknown): void {
  chosenSource = typeof id === 'string' ? id.slice(0, 200) : ''
}

export function installDisplayMediaHandler(isOurPage: (url: string) => boolean): void {
  session.defaultSession.setDisplayMediaRequestHandler(async (request, callback) => {
    if (!isOurPage(request.frame?.url ?? '')) {
      callback({})   // 우리 페이지가 아니면 거절
      return
    }
    const sources = await desktopCapturer.getSources({ types: ['screen', 'window'] })
    const primary = String(screen.getPrimaryDisplay().id)
    const pick = sources.find((s) => s.id === chosenSource)
      ?? sources.find((s) => s.id.startsWith('screen:') && s.display_id === primary)
      ?? sources.find((s) => s.id.startsWith('screen:'))
    chosenSource = ''
    callback(pick ? { video: pick } : {})
  })
}

/** 오버레이 보이기/숨기기. 주 모니터 전체를 덮는 투명 · 클릭 통과 · 항상 위 창 */
export function showOverlay(on: boolean, base: URL, webPreferences: Electron.WebPreferences): void {
  if (!on) {
    overlay?.close()
    overlay = null
    return
  }
  if (overlay) return
  const { bounds } = screen.getPrimaryDisplay()
  overlay = new BrowserWindow({
    ...bounds,
    frame: false,
    transparent: true,
    resizable: false,
    movable: false,
    focusable: false,
    skipTaskbar: true,
    hasShadow: false,
    alwaysOnTop: true,
    webPreferences,
  })
  // Windows 는 새 창을 작업 표시줄 위(작업 영역)로 줄여서 만든다 → 공유 화면(모니터 전체)과 그림 위치가 어긋남. 만든 뒤 다시 맞춤
  overlay.setBounds(bounds)
  overlay.setAlwaysOnTop(true, 'screen-saver')
  overlay.setIgnoreMouseEvents(true)
  overlay.setContentProtection(true)   // 오버레이 자체는 공유 화면에 다시 찍히지 않게
  void overlay.loadURL(new URL('/overlay', base).href)
  overlay.on('closed', () => {
    overlay = null
  })
}

/** 학생 화면이 받은 그림 메시지를 오버레이 창으로 넘긴다 */
export function forwardDraw(message: unknown): void {
  if (typeof message !== 'string' || message.length > 200_000) return
  overlay?.webContents.send('overlay-draw', message)
}
