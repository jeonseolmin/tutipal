import { useEffect } from 'react'
import Mascot from '../components/Mascot'

/**
 * 시스템 브라우저에서 앱 로그인을 마친 뒤 (서버 AppLoginSuccessHandler → 여기).
 * tutipal://auth?code= 로 앱을 다시 연다. 코드는 60초 1회용이고, 앱이 가진 비밀값 없이는 쓸 수 없다.
 */
export default function AppLoginDonePage() {
  const code = new URLSearchParams(location.search).get('code') ?? ''
  const valid = /^[A-Za-z0-9_-]{43}$/.test(code)
  const deepLink = `tutipal://auth?code=${code}`

  useEffect(() => {
    if (valid) location.href = deepLink
  }, [valid, deepLink])

  return (
    <div className="center-page">
      <div className="card session-card join-card">
        <Mascot mood={valid ? 'happy' : 'curious'} size={120} float={valid} />
        {valid ? (
          <>
            <h1>로그인 완료!</h1>
            <p className="muted">TUTIPAL 앱으로 돌아가요. 브라우저가 앱을 열지 물어보면 "열기" 를 눌러 주세요.</p>
            <a className="btn btn-primary btn-lg" href={deepLink}>앱 열기</a>
            <p className="muted small">앱이 열리면 이 탭은 닫아도 돼요.</p>
          </>
        ) : (
          <>
            <h1>로그인 정보가 없어요</h1>
            <p className="muted">앱에서 로그인을 다시 시작해 주세요.</p>
          </>
        )}
      </div>
    </div>
  )
}
