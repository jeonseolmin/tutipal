const { contextBridge, ipcRenderer } = require('electron')

// 화면(frontend)에 공개하는 기능은 이것뿐. 실제로 할지 말지는 메인 프로세스가 보낸 쪽 주소를 확인하고 정한다.
contextBridge.exposeInMainWorld('desktop', {
  isDesktop: true,
  openChat: (topic) => ipcRenderer.send('open-chat', typeof topic === 'string' ? topic : ''),
  toggleWidget: () => ipcRenderer.send('toggle-widget'),
  // 목업 02: 시스템 브라우저로 소셜 로그인 → tutipal:// 딥링크로 돌아옴
  loginInBrowser: (provider) => ipcRenderer.send('login-in-browser', provider),
  // 목업 04 · 07 · 08: 화면 분석 (고른 창만, 이미지는 저장 안 함)
  openScreenConsent: () => ipcRenderer.send('open-screen-consent'),
  openReview: (id) => ipcRenderer.send('open-review', id),
  screenSources: () => ipcRenderer.invoke('screen-sources'),
  getScreenSettings: () => ipcRenderer.invoke('screen-settings-get'),
  setScreenSettings: (s) => ipcRenderer.invoke('screen-settings-set', s),
  captureScreen: () => ipcRenderer.invoke('screen-capture'),
  // 4단계 화면공유: 다음 getDisplayMedia 에서 공유할 창 (빈 값 = 주 모니터), 학생 PC 그림 오버레이(목업 19)
  setShareSource: (id) => ipcRenderer.send('share-source', id),
  showOverlay: (on) => ipcRenderer.send('overlay-show', on === true),
  sendOverlayDraw: (message) => ipcRenderer.send('overlay-draw', message),
  onOverlayDraw: (cb) => {
    const listener = (_e, message) => {
      if (typeof message === 'string') cb(message)
    }
    ipcRenderer.on('overlay-draw', listener)
    return () => ipcRenderer.removeListener('overlay-draw', listener)
  },
  onScreenSettingsChanged: (cb) => {
    const listener = () => cb()
    ipcRenderer.on('screen-settings-changed', listener)
    return () => ipcRenderer.removeListener('screen-settings-changed', listener)
  },
})
