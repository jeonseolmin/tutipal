import { contextBridge, ipcRenderer } from 'electron'

// 화면(frontend)에 공개하는 기능은 이것뿐. 실제로 할지 말지는 메인 프로세스가 보낸 쪽 주소를 확인하고 정한다.
contextBridge.exposeInMainWorld('desktop', {
  isDesktop: true,
  openChat: (topic?: string) => ipcRenderer.send('open-chat', typeof topic === 'string' ? topic : ''),
  toggleWidget: () => ipcRenderer.send('toggle-widget'),
  // 목업 02: 시스템 브라우저로 소셜 로그인 → tutipal:// 딥링크로 돌아옴
  loginInBrowser: (provider: string) => ipcRenderer.send('login-in-browser', provider),
  // 목업 04 · 07 · 08: 화면 분석 (고른 창만, 이미지는 저장 안 함)
  openScreenConsent: () => ipcRenderer.send('open-screen-consent'),
  openReview: (id: number) => ipcRenderer.send('open-review', id),
  screenSources: () => ipcRenderer.invoke('screen-sources'),
  getScreenSettings: () => ipcRenderer.invoke('screen-settings-get'),
  setScreenSettings: (s: unknown) => ipcRenderer.invoke('screen-settings-set', s),
  captureScreen: () => ipcRenderer.invoke('screen-capture'),
  // 4단계 화면공유: 다음 getDisplayMedia 에서 공유할 창 (빈 값 = 주 모니터), 학생 PC 그림 오버레이(목업 19)
  setShareSource: (id: string) => ipcRenderer.send('share-source', id),
  showOverlay: (on: boolean) => ipcRenderer.send('overlay-show', on === true),
  sendOverlayDraw: (message: string) => ipcRenderer.send('overlay-draw', message),
  onOverlayDraw: (cb: (message: string) => void) => {
    const listener = (_e: unknown, message: unknown) => {
      if (typeof message === 'string') cb(message)
    }
    ipcRenderer.on('overlay-draw', listener)
    return () => ipcRenderer.removeListener('overlay-draw', listener)
  },
  onScreenSettingsChanged: (cb: () => void) => {
    const listener = () => cb()
    ipcRenderer.on('screen-settings-changed', listener)
    return () => ipcRenderer.removeListener('screen-settings-changed', listener)
  },
})
