import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { sessions } from '../api/sessions'
import Mascot from '../components/Mascot'

/** 목업 12 세션 참여: 선생님이 알려준 6자리 코드 */
export default function JoinSessionPage() {
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    if (code.length !== 6 || busy) return
    setBusy(true)
    setError('')
    try {
      const joined = await sessions.join(code)
      navigate(`/sessions/${joined.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '참여하지 못했어요')
      setBusy(false)
    }
  }

  return (
    <div className="center-page">
      <form className="card session-card join-card" onSubmit={onSubmit}>
        <Mascot mood="happy" size={120} float />
        <h1>방 참여하기</h1>
        <input
          className="code-input"
          value={code}
          onChange={(e) => setCode(e.target.value.replace(/[^0-9a-z]/gi, '').toUpperCase().slice(0, 6))}
          placeholder="A7K2Q9"
          aria-label="초대 코드 6자리"
          autoComplete="off"
          autoFocus
        />
        <button className="btn btn-primary btn-lg" disabled={code.length !== 6 || busy}>
          {busy ? '들어가는 중…' : '입장'}
        </button>
        {error ? <p className="error-text">{error}</p> : <p className="muted">선생님이 알려준 코드를 입력하세요</p>}
      </form>
    </div>
  )
}
