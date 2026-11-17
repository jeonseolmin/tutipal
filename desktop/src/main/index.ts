import { readFileSync } from 'node:fs'
import path from 'node:path'
import {
  app, BrowserWindow, ipcMain, Menu, nativeImage, screen, session, shell, Tray,
  type IpcMainEvent, type IpcMainInvokeEvent, type WebContents,
} from 'electron'
import { finishBrowserLogin, startBrowserLogin } from './applogin'
import { capture, listSources, loadSettings, saveSettings } from './screen'
import { chooseShareSource, forwardDraw, installDisplayMediaHandler, showOverlay } from './share'

/**
 * 서버 주소: 화면은 여기서 받아 온다 (웹과 같은 React 화면, 같은 세션 쿠키).
 * 개발은 환경변수 APP_BASE_URL, 설치 파일은 빌드할 때 package.json 의 tutipal.serverUrl 에 넣은 값
 */
function serverUrl(): string {
  if (process.env.APP_BASE_URL) return process.env.APP_BASE_URL
  try {
    const pkg = JSON.parse(readFileSync(path.join(app.getAppPath(), 'package.json'), 'utf8')) as { tutipal?: { serverUrl?: string } }
    if (pkg.tutipal?.serverUrl) return pkg.tutipal.serverUrl
  } catch {
    // 기본값
  }
  return 'http://localhost'
}
const BASE = new URL(serverUrl())
const TOPICS = new Set(['', 'general', 'coding', 'english', 'math'])
const ASSETS = path.join(__dirname, '..', '..', 'assets')
const PROTOCOL = 'tutipal'

let mainWindow: BrowserWindow | null = null
let widgetWindow: BrowserWindow | null = null
let tray: Tray | null = null

function isOurPage(url: string): boolean {
  try {
    return new URL(url).origin === BASE.origin
  } catch {
    return false
  }
}

function openOutside(url: string): void {
  if (/^https?:\/\//.test(url)) void shell.openExternal(url)
}

/**
 * 우리 서버 밖으로 나가지 못하게, 바깥 주소는 기본 브라우저로.
 * 소셜 로그인도 앱 안에서 열지 않고 브라우저 + 딥링크로 (구글이 앱 안 로그인을 막아서 전부 같은 방식으로 통일)
 */
function harden(contents: WebContents): void {
  const guard = (event: Electron.Event, url: string) => {
    if (!isOurPage(url)) {
      event.preventDefault()
      openOutside(url)
    }
  }
  contents.on('will-navigate', guard)
  contents.on('will-redirect', guard)
  contents.setWindowOpenHandler(({ url }) => {
    openOutside(url)
    return { action: 'deny' }
  })
}

const webPreferences = {
  preload: path.join(__dirname, '..', 'preload', 'index.js'),
  contextIsolation: true,
  sandbox: true,
  nodeIntegration: false,
}

function createMainWindow(): BrowserWindow {
  const win = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 900,
    minHeight: 640,
    title: 'TUTIPAL',
    backgroundColor: '#eef3fa',
    icon: path.join(ASSETS, 'icon.png'),
    autoHideMenuBar: true,
    webPreferences,
  })
  harden(win.webContents)
  void win.loadURL(BASE.href)
  win.on('closed', () => {
    mainWindow = null
    app.quit()
  })
  return win
}

function createWidgetWindow(): BrowserWindow {
  const width = 380
  const height = 340
  const { workArea } = screen.getPrimaryDisplay()
  const win = new BrowserWindow({
    width,
    height,
    x: workArea.x + workArea.width - width - 16,
    y: workArea.y + workArea.height - height - 8,
    frame: false,
    transparent: true,
    resizable: false,
    alwaysOnTop: true,
    skipTaskbar: true,
    hasShadow: false,
    webPreferences,
  })
  win.setAlwaysOnTop(true, 'floating')
  harden(win.webContents)
  void win.loadURL(new URL('/widget', BASE).href)
  win.on('closed', () => {
    widgetWindow = null
  })
  return win
}

function showMain(urlPath?: string): void {
  if (!mainWindow) mainWindow = createMainWindow()
  if (urlPath) void mainWindow.loadURL(new URL(urlPath, BASE).href)
  if (mainWindow.isMinimized()) mainWindow.restore()
  mainWindow.show()
  mainWindow.focus()
}

function toggleWidget(): void {
  if (!widgetWindow) widgetWindow = createWidgetWindow()
  else if (widgetWindow.isVisible()) widgetWindow.hide()
  else widgetWindow.show()
}

/** 화면이 보낸 요청은 우리 서버 페이지에서 온 것만 받는다 */
function fromOurPage(event: IpcMainEvent | IpcMainInvokeEvent): boolean {
  return isOurPage(event.senderFrame?.url ?? '')
}

/** tutipal://auth?code= 로 돌아왔을 때: 세션 쿠키로 교환하고 두 창을 새로 읽음 */
async function onDeepLink(url: string | undefined): Promise<void> {
  if (!url?.startsWith(`${PROTOCOL}://`)) return
  showMain()
  const ok = await finishBrowserLogin(BASE, url).catch(() => false)
  if (ok) {
    void mainWindow?.loadURL(BASE.href)
    widgetWindow?.reload()
  }
}

function createTray(): void {
  tray = new Tray(nativeImage.createFromPath(path.join(ASSETS, 'tray.png')))
  tray.setToolTip('TUTIPAL · AI 학습 도우미')
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: '열기', click: () => showMain() },
    { label: '캐릭터 위젯 보이기/숨기기', click: () => toggleWidget() },
    { label: '화면 분석 설정', click: () => showMain('/screen-consent') },
    { type: 'separator' },
    { label: '종료', click: () => app.quit() },
  ]))
  tray.on('click', () => showMain())
}

// tutipal:// 을 이 앱으로 연결 (Windows: 사용자 레지스트리 HKCU 에 등록). 개발 모드는 electron.exe + 앱 폴더로 등록해야 함.
// argv[1] 을 쓰면 `electron --inspect .` 처럼 옵션이 앞에 올 때 옵션이 앱 경로로 등록돼 버림 (실제로 겪음) → getAppPath
if (process.defaultApp) {
  app.setAsDefaultProtocolClient(PROTOCOL, process.execPath, [app.getAppPath()])
} else {
  app.setAsDefaultProtocolClient(PROTOCOL)
}

if (!app.requestSingleInstanceLock()) {
  app.quit()
} else {
  // Windows: 딥링크는 "두 번째 실행" 의 명령줄 인자로 들어온다
  app.on('second-instance', (_event, argv) => {
    showMain()
    void onDeepLink(argv.find((a) => a.startsWith(`${PROTOCOL}://`)))
  })
  // macOS
  app.on('open-url', (event, url) => {
    event.preventDefault()
    void onDeepLink(url)
  })

  app.whenReady().then(() => {
    // 권한은 화면 공유(display-capture, 우리 페이지에서만)만 허용. 카메라 · 마이크 · 알림 등은 모두 거절.
    // 실제로 어떤 창을 공유할지는 share.ts 의 setDisplayMediaRequestHandler 가 정한다
    session.defaultSession.setPermissionRequestHandler((wc, permission, callback) =>
      callback(permission === 'display-capture' && isOurPage(wc.getURL())))

    ipcMain.on('open-chat', (event, topic: unknown) => {
      if (!fromOurPage(event)) return
      const t = typeof topic === 'string' && TOPICS.has(topic) ? topic : ''
      showMain(t ? `/chat?topic=${t}` : '/chat')
    })
    ipcMain.on('toggle-widget', (event) => {
      if (fromOurPage(event)) toggleWidget()
    })
    ipcMain.on('open-screen-consent', (event) => {
      if (fromOurPage(event)) showMain('/screen-consent')
    })
    ipcMain.on('open-review', (event, id: unknown) => {
      if (fromOurPage(event) && Number.isSafeInteger(id) && (id as number) > 0) showMain(`/review?explain=${id as number}`)
    })
    ipcMain.on('login-in-browser', (event, provider: unknown) => {
      if (fromOurPage(event)) startBrowserLogin(BASE, provider)
    })
    ipcMain.handle('screen-sources', (event) => (fromOurPage(event) ? listSources() : []))
    ipcMain.handle('screen-settings-get', (event) => (fromOurPage(event) ? loadSettings() : null))
    ipcMain.handle('screen-settings-set', (event, settings: unknown) => {
      if (!fromOurPage(event)) return null
      const saved = saveSettings(settings)
      widgetWindow?.webContents.send('screen-settings-changed')
      return saved
    })
    ipcMain.handle('screen-capture', (event) => (fromOurPage(event) ? capture() : { status: 'off' }))
    // 4단계 화면공유: 공유할 창 고르기 → getDisplayMedia 허용, 학생 PC 그림 오버레이
    installDisplayMediaHandler(isOurPage)
    ipcMain.on('share-source', (event, id: unknown) => {
      if (fromOurPage(event)) chooseShareSource(id)
    })
    ipcMain.on('overlay-show', (event, on: unknown) => {
      if (fromOurPage(event)) showOverlay(on === true, BASE, webPreferences)
    })
    ipcMain.on('overlay-draw', (event, message: unknown) => {
      if (fromOurPage(event)) forwardDraw(message)
    })

    mainWindow = createMainWindow()
    widgetWindow = createWidgetWindow()
    createTray()
  })
}
