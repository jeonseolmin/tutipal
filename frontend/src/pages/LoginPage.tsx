import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { ApiRequestError, api } from '../api/client'
import type { SocialProvider } from '../api/types'
import { useAuth } from '../auth'
import Mascot from '../components/Mascot'

const PROVIDERS: { id: SocialProvider; label: string; className: string; mark: string }[] = [
  { id: 'kakao', label: '카카오로 시작하기', className: 'social kakao', mark: '💬' },
  { id: 'naver', label: '네이버로 시작하기', className: 'social naver', mark: 'N' },
  { id: 'google', label: 'Google로 시작하기', className: 'social google', mark: 'G' },
]

// 개발용 로그인 버튼은 내 PC 에서 열었을 때만 보임 (서버도 app.dev-login=true 여야 동작)
const LOCAL = ['localhost', '127.0.0.1'].includes(window.location.hostname)

/** 목업 01 로그인 */
export default function LoginPage() {
  const { me, refresh } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  /** 앱: 브라우저로 로그인하러 간 소셜 (목업 02) */
  const [waiting, setWaiting] = useState<SocialProvider | 'dev' | null>(null)

  if (me) return <Navigate to="/" replace />

  // 앱에서는 소셜 로그인을 시스템 브라우저에서 하고 tutipal:// 로 돌아온다 (구글이 앱 안 로그인을 막음)
  function loginInBrowser(provider: SocialProvider | 'dev') {
    setWaiting(provider)
    window.desktop?.loginInBrowser(provider)
  }

  if (waiting && window.desktop) {
    return (
      <div className="center-page">
        <div className="card login-card">
          <Mascot mood="think" size={140} float />
          <h1 className="login-title waiting-title">브라우저에서 로그인 중…</h1>
          <p className="muted">열린 브라우저에서 로그인을 마치면 앱으로 자동으로 돌아와요.</p>
          <button className="btn btn-link" onClick={() => loginInBrowser(waiting)}>브라우저 다시 열기</button>
          <button className="btn btn-sm" onClick={() => setWaiting(null)}>다른 방법으로 로그인</button>
        </div>
      </div>
    )
  }

  async function devLogin() {
    setError(null)
    try {
      // /login?dev=2 로 열면 2번 계정 (시크릿 창에서 학생 역할 시험용)
      await api.devLogin(Number(new URLSearchParams(location.search).get('dev')) || 1)
      await refresh()
      navigate('/', { replace: true })
    } catch (e) {
      setError(e instanceof ApiRequestError && e.status === 404
        ? '서버에서 개발용 로그인이 꺼져 있어요 (DEV_LOGIN=true)'
        : '로그인하지 못했어요')
    }
  }

  return (
    <div className="center-page">
      <div className="card login-card">
        <Mascot size={150} float />
        <h1 className="login-title">TUTIPAL</h1>
        <p className="muted login-sub">AI 학습 도우미 · 화면을 보며 함께 공부해요</p>
        <div className="login-buttons">
          {PROVIDERS.map((p) => window.desktop ? (
            <button key={p.id} className={p.className} onClick={() => loginInBrowser(p.id)}>
              <span className="social-mark" aria-hidden="true">{p.mark}</span>
              {p.label}
            </button>
          ) : (
            <a key={p.id} className={p.className} href={`/oauth2/authorization/${p.id}`}>
              <span className="social-mark" aria-hidden="true">{p.mark}</span>
              {p.label}
            </a>
          ))}
        </div>
        {LOCAL && (
          <button className="btn btn-sm dev-login" onClick={() => void devLogin()}>
            개발용 로그인 (내 PC 전용)
          </button>
        )}
        {LOCAL && window.desktop && (
          <button className="btn btn-sm dev-login" onClick={() => loginInBrowser('dev')}>
            개발용 로그인 · 브라우저 경유 (딥링크 시험)
          </button>
        )}
        {error && <p className="error-text">{error}</p>}
      </div>
    </div>
  )
}
