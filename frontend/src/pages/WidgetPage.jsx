import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiRequestError } from '../api/client'
import { reviewApi } from '../api/review'
import { screenApi } from '../api/screen'
import Icon from '../components/Icon'
import Mascot, {  } from '../components/Mascot'

/** 화면이 그대로면 서버를 부르지 않으므로, 40초마다 한 장 찍어 비교만 한다 */
const SUMMARY_EVERY_MS = 40_000

/**
 * 목업 07 캐릭터 위젯 (Electron 의 투명 · 항상 위 작은 창).
 * 화면 분석이 켜져 있으면 고른 창을 주기적으로 요약해 말풍선에 보여 주고(07),
 * "질문하기" 로 지금 화면에 대해 물어볼 수 있다(08). 이미지는 저장하지 않는다.
 */
export default function WidgetPage() {
  const navigate = useNavigate()
  const desktop = window.desktop
  const [settings, setSettings] = useState(null)
  const [paused, setPaused] = useState(false)
  const [asking, setAsking] = useState(false)
  const [question, setQuestion] = useState('')
  const [text, setText] = useState('')
  const [mood, setMood] = useState('happy')
  const busy = useRef(false)
  /** 동의한 창이 닫혔거나 다시 열려서 못 찾음 → 다시 골라야 함 */
  const [gone, setGone] = useState(false)
  const lastSeen = useRef(Date.now())
  const [suggestion, setSuggestion] = useState(null)

  /** 복습 기록 (동의 안 했으면 서버가 그냥 무시). 실패해도 위젯은 계속 */
  const observe = async (summary) => {
    const seconds = Math.round((Date.now() - lastSeen.current) / 1000)
    lastSeen.current = Date.now()
    const show = (r) => r.suggestion && setSuggestion(r.suggestion)
    try {
      if (summary) {
        show(await reviewApi.observe(null, seconds))   // 지금까지 본 시간은 직전 화면(묶음)에
        show(await reviewApi.observe(summary, 0))      // 새 화면은 0초부터
      } else {
        show(await reviewApi.observe(null, seconds))
      }
    } catch {
      // 복습 기록 실패는 위젯 동작에 영향 없음
    }
  }

  useEffect(() => {
    document.documentElement.classList.add('transparent')
    return () => document.documentElement.classList.remove('transparent')
  }, [])

  // 화면 분석 설정 (메인 창에서 바꾸면 알려 줌)
  useEffect(() => {
    if (!desktop) return
    const load = () => desktop.getScreenSettings().then(setSettings, () => undefined)
    load()
    return desktop.onScreenSettingsChanged(load)
  }, [desktop])

  const explain = (e) => {
    setMood('curious')
    if (e instanceof ApiRequestError && e.status === 401) setText('메인 창에서 로그인하면 화면을 같이 볼 수 있어요.')
    else setText(e instanceof Error ? e.message : '지금은 화면을 보지 못했어요')
  }

  /** 한 번 찍어서 (바뀌었으면 / 질문이면) 서버에 묻는다 */
  const look = useCallback(async (ask) => {
    if (!desktop || busy.current) return
    busy.current = true
    try {
      const cap = await desktop.captureScreen()
      if (cap.status === 'off') return
      setGone(cap.status === 'gone')
      if (cap.status === 'gone') {
        setMood('curious')
        setText('보던 창이 닫혔어요. 볼 창을 다시 골라 주세요.')
        return
      }
      if (!ask && !cap.changed) {
        void observe(null)   // 같은 화면을 계속 보는 중 → 본 시간만 더함
        return
      }
      setMood(ask ? 'think' : 'listen')
      const { text: answer } = await screenApi.describe(cap.image, ask)
      setText(answer)
      setMood(ask ? 'talk' : 'happy')
      if (!ask) void observe(answer)
    } catch (e) {
      // 자동 요약 중 GPU 가 바쁘면(503) 조용히 다음 차례에
      if (!ask && e instanceof ApiRequestError && e.status === 503) return
      explain(e)
    } finally {
      busy.current = false
    }
  }, [desktop])

  const watching = !!settings?.enabled && !paused && !asking
  useEffect(() => {
    if (!watching) return
    void look()
    const t = setInterval(() => void look(), SUMMARY_EVERY_MS)
    return () => clearInterval(t)
  }, [watching, look])

  async function onAsk(e) {
    e.preventDefault()
    const q = question.trim()
    if (!q) return
    setText('화면을 보면서 생각하는 중…')
    setAsking(false)
    setQuestion('')
    await look(q)
  }

  function openAsk() {
    // 화면 분석이 꺼져 있으면 일반 챗봇으로
    if (!settings?.enabled) {
      if (desktop) desktop.openChat()
      else navigate('/chat')
      return
    }
    setAsking(true)
  }

  const off = !settings?.enabled
  const message = off
    ? '화면 분석을 켜면 보고 있는 창을 요약해 드려요. 궁금한 건 질문하기!'
    : paused
      ? '화면 분석을 잠시 멈췄어요. 다시 켜려면 ▶ 를 눌러 주세요.'
      : text || '화면을 살펴보는 중이에요…'

  return (
    <div className="widget">
      <div className="widget-bubble">
        {suggestion && !asking ? (
          <div className="widget-suggest">
            <p className="widget-text"><b>이 내용 자주 보셨네요, 자세히 설명해 드릴까요?</b></p>
            <div className="widget-row">
              <button className="btn btn-primary btn-sm" onClick={() => { desktop?.openReview(suggestion.id); setSuggestion(null) }}>설명 듣기</button>
              <button className="btn btn-sm" onClick={() => setSuggestion(null)}>나중에</button>
            </div>
          </div>
        ) : asking ? (
          <form className="widget-ask" onSubmit={(e) => void onAsk(e)}>
            <input value={question} onChange={(e) => setQuestion(e.target.value)} maxLength={500}
              placeholder="지금 화면에 대해 물어보세요" autoFocus aria-label="화면 질문" />
            <button className="btn btn-primary btn-sm" disabled={!question.trim()}>보내기</button>
            <button type="button" className="btn btn-sm" onClick={() => setAsking(false)} aria-label="취소">✕</button>
          </form>
        ) : (
          <p className="widget-text" aria-live="polite">{message}</p>
        )}
        <div className="widget-row">
          <span className="widget-status">
            <span className={off || paused ? 'dot off' : 'dot'} /> {off ? '꺼짐' : paused ? '일시정지' : 'AI 보는 중'}
          </span>
          {off || gone ? (
            <button className="btn btn-sm" onClick={() => desktop?.openScreenConsent()}>{off ? '화면 분석 켜기' : '창 다시 고르기'}</button>
          ) : (
            <button className="btn btn-sm" onClick={() => setPaused((p) => !p)} aria-label={paused ? '다시 시작' : '일시정지'}>
              <Icon name={paused ? 'play' : 'pause'} size={16} /> {paused ? '다시' : '일시정지'}
            </button>
          )}
          <button className="btn btn-primary btn-sm" onClick={openAsk}>
            <Icon name="chat" size={16} /> 질문하기
          </button>
        </div>
      </div>
      <div className="widget-mascot" title="끌어서 옮기기">
        <Mascot mood={off || paused ? 'sleep' : mood} size={110} float={!paused && !off} />
      </div>
    </div>
  )
}
