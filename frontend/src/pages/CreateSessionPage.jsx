import { useState } from 'react'
import { Link } from 'react-router-dom'
import { sessions } from '../api/sessions'
import Mascot from '../components/Mascot'
import SubjectPicker from '../components/SubjectPicker'

/** 목업 11 방 만들기: 이름 + 과목(드롭다운 · 직접 입력) → 초대 코드 (만든 사람이 이 방의 선생님) */
export default function CreateSessionPage() {
  const [title, setTitle] = useState('')
  const [topic, setTopic] = useState('')
  const [created, setCreated] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [copied, setCopied] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    if (!title.trim() || !topic.trim() || busy) return
    setBusy(true)
    setError('')
    try {
      setCreated(await sessions.create(title.trim(), topic.trim()))
    } catch (err) {
      setError(err instanceof Error ? err.message : '방을 만들지 못했어요')
    } finally {
      setBusy(false)
    }
  }

  async function copy() {
    if (!created) return
    await navigator.clipboard.writeText(created.inviteCode).catch(() => undefined)
    setCopied(true)
  }

  return (
    <div className="page session-create">
      <form className="card session-card" onSubmit={onSubmit}>
        <Mascot mood="happy" size={120} />
        <label className="field">
          <strong>방 이름</strong>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={50}
            placeholder="예: 자바 기초 3주차"
            disabled={!!created}
            autoFocus
          />
        </label>
        <div className="field">
          <strong>공부할 과목</strong>
          <SubjectPicker value={topic} onChange={setTopic} disabled={!!created} />
        </div>
        <button className="btn btn-primary btn-lg" disabled={!title.trim() || !topic.trim() || busy || !!created}>
          {busy ? '만드는 중…' : '만들기'}
        </button>
        {error && <p className="error-text">{error}</p>}
      </form>

      <section className="card session-card code-card" aria-live="polite">
        {created ? (
          <>
            <Mascot mood="happy" size={120} float />
            <div className="code-row">
              <span className="invite-code">{created.inviteCode}</span>
              <button type="button" className="btn" onClick={() => void copy()}>{copied ? '복사됨' : '복사'}</button>
            </div>
            <p className="muted">학생에게 이 코드를 알려 주세요. 학생은 홈 → 방 참여하기에서 입력해요.</p>
            <Link to={`/sessions/${created.id}`} className="btn btn-primary">선생님 모드로 들어가기</Link>
          </>
        ) : (
          <>
            <Mascot mood="idle" size={120} />
            <p className="muted">방을 만들면 여기에 초대 코드가 나와요</p>
          </>
        )}
      </section>
    </div>
  )
}
