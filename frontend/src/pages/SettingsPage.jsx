import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { reviewApi } from '../api/review'
import { useAuth } from '../auth'
import Icon from '../components/Icon'
import Mascot from '../components/Mascot'

const TABS = [
  { id: 'screen', icon: 'screen', label: '화면 분석' },
  { id: 'review', icon: 'book', label: '복습 기록' },
  { id: 'account', icon: 'user', label: '계정' },
]
const PROVIDER = { kakao: '카카오', naver: '네이버', google: '구글', dev: '개발용' }

/** 목업 05 설정: 화면 요약 · 복습 기록 저장 켜고 끄기, 기록 전체 삭제, 계정, 로그아웃 */
export default function SettingsPage() {
  const [tab, setTab] = useState('screen')
  return (
    <div className="page">
      <section className="settings-page">
        <nav className="card settings-menu" role="tablist" aria-label="설정 메뉴">
          {TABS.map((t) => (
            <button key={t.id} role="tab" aria-selected={tab === t.id} onClick={() => setTab(t.id)}>
              <Icon name={t.icon} /> {t.label}
            </button>
          ))}
        </nav>
        <div className="card settings-body">
          {tab === 'screen' && <ScreenTab />}
          {tab === 'review' && <ReviewTab />}
          {tab === 'account' && <AccountTab />}
        </div>
      </section>
    </div>
  )
}

function Head({ mood, title, text }) {
  return (
    <header className="settings-head">
      <Mascot mood={mood} size={110} />
      <div>
        <h1>{title}</h1>
        <p className="muted">{text}</p>
      </div>
    </header>
  )
}

/** 복습 기록 동의 (끄면 서버가 기존 기록도 지움) */
function useReviewConsent() {
  const [on, setOn] = useState(null)
  useEffect(() => {
    reviewApi.consent().then((c) => setOn(c.on), () => setOn(false))
  }, [])
  const toggle = async () => {
    const next = !on
    await reviewApi.setConsent(next)
    setOn(next)
  }
  return [on, toggle]
}

function ScreenTab() {
  const navigate = useNavigate()
  const desktop = window.desktop
  const [screen, setScreen] = useState(null)
  const [review, toggleReview] = useReviewConsent()

  useEffect(() => {
    if (!desktop) return
    const load = () => desktop.getScreenSettings().then(setScreen, () => undefined)
    load()
    return desktop.onScreenSettingsChanged?.(load)
  }, [desktop])

  // 켜기는 볼 창을 골라야 하므로 동의 화면(04)으로, 끄기는 바로
  async function toggleScreen() {
    if (!screen?.enabled) return navigate('/screen-consent')
    setScreen(await desktop.setScreenSettings({ ...screen, enabled: false }))
  }

  return (
    <>
      <Head mood="happy" title="화면 분석" text="고른 창만 보고, 화면 이미지는 저장하지 않아요. 언제든 끌 수 있어요." />
      <label className="setting-card">
        <Icon name="screen" size={28} />
        <span>
          <strong>화면 요약</strong>
          <span className="muted small">
            {!desktop ? 'TUTIPAL 데스크톱 앱에서만 쓸 수 있어요'
              : screen?.enabled ? `지금 보는 창: ${screen.sourceName}` : '켜면 고른 창의 내용을 요약해서 캐릭터가 알려 줘요'}
          </span>
          {screen?.enabled && <Link to="/screen-consent" className="btn-link small">다른 창 고르기</Link>}
        </span>
        <input type="checkbox" className="switch" checked={!!screen?.enabled} onChange={() => void toggleScreen()} disabled={!desktop || !screen} />
      </label>
      <label className="setting-card">
        <Icon name="book" size={28} />
        <span>
          <strong>복습 기록 저장</strong>
          <span className="muted small">자주 · 오래 본 내용의 요약 글만 남겨 복습을 제안해요. 끄면 지금까지 기록도 지워져요</span>
        </span>
        <input type="checkbox" className="switch" checked={!!review} onChange={() => void toggleReview()} disabled={review === null} />
      </label>
      <DeleteAll />
      <Logout />
    </>
  )
}

function ReviewTab() {
  const [count, setCount] = useState(null)
  useEffect(() => {
    reviewApi.items().then((l) => setCount(l.length), () => setCount(0))
  }, [])
  return (
    <>
      <Head mood="listen" title="복습 기록" text="화면 분석이 켜져 있을 때 자주 보거나 오래 본 내용이 모여요." />
      <div className="setting-card">
        <Icon name="book" size={28} />
        <span>
          <strong>모인 기록 {count ?? '…'}개</strong>
          <span className="muted small">설명 듣기 · 하나씩 지우기는 복습 기록 화면에서</span>
        </span>
        <Link to="/review" className="btn btn-outline">복습 기록 보기</Link>
      </div>
      <DeleteAll onDone={() => setCount(0)} />
    </>
  )
}

function AccountTab() {
  const { me } = useAuth()
  return (
    <>
      <Head mood="idle" title="계정" text="TUTIPAL 에 로그인한 계정이에요." />
      <dl className="account-list">
        <dt>이름</dt><dd>{me?.name}</dd>
        <dt>이메일</dt><dd>{me?.email ?? '없음'}</dd>
        <dt>로그인 방식</dt><dd>{PROVIDER[me?.provider] ?? me?.provider}</dd>
      </dl>
      <Logout />
    </>
  )
}

function DeleteAll({ onDone }) {
  const [confirming, setConfirming] = useState(false)
  const [done, setDone] = useState(false)
  async function run() {
    await reviewApi.deleteAll()
    setConfirming(false)
    setDone(true)
    onDone?.()
  }
  if (confirming) {
    return (
      <div className="settings-danger row-gap">
        <span className="muted small">복습 기록을 모두 지울까요? 되돌릴 수 없어요</span>
        <button className="btn btn-danger btn-sm" onClick={() => void run()}>삭제</button>
        <button className="btn btn-sm" onClick={() => setConfirming(false)}>취소</button>
      </div>
    )
  }
  return (
    <div className="settings-danger">
      <button className="btn btn-outline-danger btn-lg" onClick={() => { setDone(false); setConfirming(true) }}>
        <Icon name="trash" /> 기록 전체 삭제
      </button>
      {done && <span className="muted small">모두 지웠어요</span>}
    </div>
  )
}

function Logout() {
  const { logout } = useAuth()
  return (
    <button className="btn-link settings-logout" onClick={() => void logout()}>
      <Icon name="logout" /> 로그아웃
    </button>
  )
}
