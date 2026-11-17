const { createHash } = require('node:crypto')
const { readFileSync, writeFileSync } = require('node:fs')
const path = require('node:path')
const { app, BrowserWindow, desktopCapturer } = require('electron')

const OFF = { enabled: false, sourceId: '', sourceName: '' }
const file = () => path.join(app.getPath('userData'), 'screen.json')
let lastHash = ''

function loadSettings() {
  try {
    const s = JSON.parse(readFileSync(file(), 'utf8'))
    return { enabled: s.enabled === true, sourceId: String(s.sourceId ?? ''), sourceName: String(s.sourceName ?? '') }
  } catch {
    return OFF
  }
}

/** 화면에서 온 값은 모양을 다시 확인해서 저장 */
function saveSettings(input) {
  const s = input ?? {}
  const next = {
    enabled: s.enabled === true,
    sourceId: typeof s.sourceId === 'string' ? s.sourceId.slice(0, 200) : '',
    sourceName: typeof s.sourceName === 'string' ? s.sourceName.slice(0, 200) : '',
  }
  if (!next.sourceId) next.enabled = false
  writeFileSync(file(), JSON.stringify(next))
  lastHash = ''
  return next
}

/** 우리 앱 창(메인 · 위젯)은 목록에서 뺀다 */
function ownIds() {
  return new Set(BrowserWindow.getAllWindows().map((w) => w.getMediaSourceId()))
}

/** 목업 04: 고를 수 있는 창 목록 (작은 미리보기 포함) */
async function listSources() {
  const own = ownIds()
  const sources = await desktopCapturer.getSources({ types: ['window', 'screen'], thumbnailSize: { width: 320, height: 200 } })
  return sources
    .filter((s) => !own.has(s.id) && s.name.trim() !== '')
    .map((s) => ({ id: s.id, name: s.id.startsWith('screen:') ? `전체 화면 (${s.name})` : s.name, thumbnail: s.thumbnail.toDataURL() }))
}

/**
 * 고른 창을 한 장 찍는다 (JPEG base64). changed = 지난번과 달라졌는지 (같으면 위젯이 서버 호출을 건너뜀).
 * ponytail: getSources 가 모든 창의 미리보기를 만들어 무거움 → 위젯이 40초마다만 부름
 */
async function capture() {
  const settings = loadSettings()
  if (!settings.enabled) return { status: 'off' }
  const sources = await desktopCapturer.getSources({ types: ['window', 'screen'], thumbnailSize: { width: 1280, height: 800 } })
  // 동의한 그 창(id)만. 이름이 같은 다른 창(다른 문서 · 다른 사람 창)을 대신 찍지 않는다 (코드 리뷰 지적).
  // 창 id 는 앱을 다시 켜거나 창을 다시 열면 바뀌므로 그때는 학생이 다시 고른다 (위젯이 "다시 골라 주세요" 안내)
  const src = sources.find((s) => s.id === settings.sourceId)
  if (!src || src.thumbnail.isEmpty()) return { status: 'gone' }
  const jpeg = src.thumbnail.toJPEG(70)
  const hash = createHash('sha1').update(jpeg).digest('hex')
  const changed = hash !== lastHash
  lastHash = hash
  return { status: 'ok', image: jpeg.toString('base64'), changed }
}

module.exports = { loadSettings, saveSettings, listSources, capture }
