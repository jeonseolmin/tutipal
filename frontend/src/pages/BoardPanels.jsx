import { useCallback, useEffect, useRef, useState } from 'react'
import { materials, notices, posts } from '../api/board'
import Icon from '../components/Icon'
import Mascot from '../components/Mascot'

const errText = (e, fallback) => (e instanceof Error ? e.message : fallback)
const when = (iso) => new Date(iso).toLocaleString('ko-KR', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
const kb = (n) => (n < 1024 * 1024 ? `${Math.max(1, Math.round(n / 1024))}KB` : `${(n / 1024 / 1024).toFixed(1)}MB`)

/** 목업 28: 자료 (선생님 업로드 · 삭제, 모두 내려받기). 올린 자료는 세션 챗봇이 근거로 쓴다 */
export function MaterialsPanel({ sessionId, teacher, reloadKey }) {
  const [list, setList] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [drag, setDrag] = useState(false)
  const inputRef = useRef(null)

  const load = useCallback(() => {
    materials.list(sessionId).then(setList, (e) => setError(errText(e, '불러오지 못했어요')))
  }, [sessionId])
  useEffect(load, [load, reloadKey])

  async function upload(files) {
    if (!files?.length) return
    setBusy(true)
    setError('')
    try {
      for (const f of Array.from(files)) await materials.upload(sessionId, f)
      load()
    } catch (e) {
      setError(errText(e, '올리지 못했어요'))
    } finally {
      setBusy(false)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  function onDrop(e) {
    e.preventDefault()
    setDrag(false)
    void upload(e.dataTransfer.files)
  }

  return (
    <section className="card session-body">
      <strong className="section-title"><Icon name="book" size={18} /> 자료</strong>
      {teacher && (
        <label className={`dropzone${drag ? ' over' : ''}`} onDragOver={(e) => { e.preventDefault(); setDrag(true) }}
          onDragLeave={() => setDrag(false)} onDrop={onDrop}>
          <Mascot mood="happy" size={70} />
          <span>
            <strong>{busy ? '올리는 중…' : '자료 업로드'}</strong>
            <span className="muted small">txt · md · pdf, 10MB 이하. 끌어다 놓거나 눌러서 고르기. 학생 챗봇이 이 자료를 근거로 답해요</span>
          </span>
          <input ref={inputRef} type="file" accept=".txt,.md,.pdf" multiple disabled={busy} onChange={(e) => void upload(e.target.files)} />
        </label>
      )}
      {error && <p className="error-text">{error}</p>}
      {list.length === 0 ? <p className="muted">아직 올린 자료가 없어요</p> : (
        <table className="table">
          <thead><tr><th>파일</th><th>크기 · 올린 때</th><th>상태</th><th /></tr></thead>
          <tbody>
            {list.map((m) => (
              <tr key={m.id}>
                <td><a href={materials.fileUrl(m.id)} download>{m.filename}</a></td>
                <td className="muted small">{kb(m.sizeBytes)} · {when(m.createdAt)}</td>
                <td>
                  {m.status === 'PROCESSING' && <span className="badge badge-warn">처리 중</span>}
                  {m.status === 'DONE' && <span className="badge badge-ok" title={`검색 조각 ${m.chunkCount}개`}>완료</span>}
                  {m.status === 'FAILED' && <span className="badge badge-bad" title={m.error ?? ''}>실패</span>}
                  {m.status === 'FAILED' && m.error && <span className="muted small"> {m.error}</span>}
                </td>
                <td>{teacher && <button className="btn btn-sm" onClick={() => void materials.remove(m.id).then(load)}>삭제</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}

/** 목업 29 왼쪽: 공지 (선생님 쓰기 · "학생에게 알림") */
export function NoticesPanel({ sessionId, teacher, reloadKey }) {
  const [list, setList] = useState([])
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [alert, setAlert] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    notices.list(sessionId).then(setList, (e) => setError(errText(e, '불러오지 못했어요')))
  }, [sessionId])
  useEffect(load, [load, reloadKey])

  async function onSubmit(e) {
    e.preventDefault()
    try {
      await notices.create(sessionId, title.trim(), body.trim(), alert)
      setTitle('')
      setBody('')
      load()
    } catch (err) {
      setError(errText(err, '올리지 못했어요'))
    }
  }

  return (
    <section className="card session-body">
      <strong className="section-title">📢 공지</strong>
      {teacher && (
        <form className="notice-form" onSubmit={(e) => void onSubmit(e)}>
          <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={100} placeholder="제목" required />
          <textarea className="input" rows={3} value={body} onChange={(e) => setBody(e.target.value)} maxLength={3000} placeholder="내용" required />
          <div className="row-between">
            <label className="check-row"><input type="checkbox" checked={alert} onChange={(e) => setAlert(e.target.checked)} /> 학생에게 알림</label>
            <button className="btn btn-primary" disabled={!title.trim() || !body.trim()}>공지 올리기</button>
          </div>
        </form>
      )}
      {error && <p className="error-text">{error}</p>}
      {list.length === 0 && <p className="muted">공지가 없어요</p>}
      {list.map((n) => (
        <article key={n.id} className="notice-item">
          <div className="row-between">
            <strong>{n.title}</strong>
            <span className="muted small">{when(n.createdAt)}{teacher && <button className="btn-link small" onClick={() => void notices.remove(n.id).then(load)}> 삭제</button>}</span>
          </div>
          <p>{n.body}</p>
        </article>
      ))}
    </section>
  )
}

/** 목업 29 오른쪽: 게시판 (참가자 누구나 글 · 댓글) */
export function BoardPanel({ sessionId, teacher, reloadKey }) {
  const [list, setList] = useState([])
  const [open, setOpen] = useState(null)
  const [writing, setWriting] = useState(false)
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [comment, setComment] = useState('')
  const [error, setError] = useState('')

  const load = useCallback(() => {
    posts.list(sessionId).then(setList, (e) => setError(errText(e, '불러오지 못했어요')))
  }, [sessionId])
  useEffect(load, [load, reloadKey])
  useEffect(() => {
    // 알림(reloadKey)이 오면 열어 둔 글도 다시 읽기 (댓글이 달렸을 수 있음)
    if (open) posts.get(open.id).then(setOpen, () => setOpen(null))
  }, [reloadKey])

  const act = async (fn) => {
    setError('')
    try {
      await fn()
    } catch (e) {
      setError(errText(e, '처리하지 못했어요'))
    }
  }

  if (open) {
    return (
      <section className="card session-body">
        <div className="row-between">
          <button className="btn btn-sm" onClick={() => { setOpen(null); load() }}>← 목록</button>
          {(open.mine || teacher) && <button className="btn btn-sm" onClick={() => void act(async () => { await posts.remove(open.id); setOpen(null); load() })}>글 삭제</button>}
        </div>
        <h3 className="post-title">{open.title}</h3>
        <p className="muted small">{open.authorName} · {when(open.createdAt)}</p>
        <p className="post-body">{open.body}</p>
        <strong className="section-title"><Icon name="chat" size={18} /> 댓글 {open.comments.length}</strong>
        {open.comments.map((c) => (
          <div key={c.id} className="comment">
            <strong>{c.authorName}</strong> <span>{c.body}</span>
            <span className="muted small"> {when(c.createdAt)}</span>
            {(c.mine || teacher) && <button className="btn-link small" onClick={() => void act(async () => { await posts.removeComment(c.id); setOpen(await posts.get(open.id)) })}> 삭제</button>}
          </div>
        ))}
        <form className="row-gap" onSubmit={(e) => { e.preventDefault(); void act(async () => { setOpen(await posts.comment(open.id, comment.trim())); setComment('') }) }}>
          <input className="input" value={comment} onChange={(e) => setComment(e.target.value)} maxLength={1000} placeholder="댓글" />
          <button className="btn btn-primary" disabled={!comment.trim()}>등록</button>
        </form>
        {error && <p className="error-text">{error}</p>}
      </section>
    )
  }

  return (
    <section className="card session-body">
      <div className="row-between">
        <strong className="section-title"><Icon name="chat" size={18} /> 게시판</strong>
        <button className="btn btn-sm" onClick={() => setWriting((w) => !w)}>{writing ? '닫기' : '글쓰기'}</button>
      </div>
      {writing && (
        <form className="notice-form" onSubmit={(e) => { e.preventDefault(); void act(async () => { await posts.create(sessionId, title.trim(), body.trim()); setTitle(''); setBody(''); setWriting(false); load() }) }}>
          <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={100} placeholder="제목" required />
          <textarea className="input" rows={3} value={body} onChange={(e) => setBody(e.target.value)} maxLength={3000} placeholder="질문이나 나누고 싶은 내용" required />
          <button className="btn btn-primary" disabled={!title.trim() || !body.trim()}>올리기</button>
        </form>
      )}
      {error && <p className="error-text">{error}</p>}
      {list.length === 0 && <p className="muted">아직 글이 없어요</p>}
      {list.map((p) => (
        <button key={p.id} className="post-row" onClick={() => void posts.get(p.id).then(setOpen)}>
          <span className="post-icon"><Icon name="book" size={20} /></span>
          <span className="post-text"><strong>{p.title}</strong><span className="muted small">{p.authorName} · {when(p.createdAt)}</span></span>
          <span className="muted"><Icon name="chat" size={16} /> {p.commentCount}</span>
        </button>
      ))}
    </section>
  )
}
