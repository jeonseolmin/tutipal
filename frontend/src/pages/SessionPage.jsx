import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { quizzes } from '../api/quizzes'
import { useSessionEvents } from '../api/realtime'
import { sessions } from '../api/sessions'
import Icon from '../components/Icon'
import Mascot from '../components/Mascot'
import { ChatPanel } from './ChatPage'
import SubjectPicker from '../components/SubjectPicker'
import { BoardPanel, MaterialsPanel, NoticesPanel } from './BoardPanels'
import { StudentHelp, StudentLiveTab, TeacherHelpTab, TeacherShareTab } from './LivePanels'
import { notices } from '../api/board'
import { StudentQuizModal, TeacherQuizTab } from './QuizPanels'

/** 세션 화면: 내 역할에 따라 학생 홈(목업 13) 또는 선생님 모드(목업 20 · 21) */
export default function SessionPage() {
  const id = Number(useParams().id)
  const [session, setSession] = useState(null)
  const [error, setError] = useState('')

  const reload = useCallback(() => {
    sessions.get(id).then(setSession, (e) => setError(e instanceof Error ? e.message : '방을 열지 못했어요'))
  }, [id])
  useEffect(reload, [reload])

  if (error) {
    return (
      <div className="center-page">
        <div className="card session-card">
          <Mascot mood="curious" size={110} />
          <p>{error}</p>
          <Link to="/" className="btn">홈으로</Link>
        </div>
      </div>
    )
  }
  if (!session) return <p className="muted">불러오는 중…</p>
  if (session.ended) {
    return (
      <div className="center-page">
        <div className="card session-card">
          <Mascot mood="sleep" size={110} />
          <p>「{session.title}」 방은 닫혔어요</p>
          <Link to="/" className="btn">홈으로</Link>
        </div>
      </div>
    )
  }
  return session.myRole === 'TEACHER'
    ? <TeacherView session={session} onChange={setSession} reload={reload} />
    : <StudentView session={session} reload={reload} />
}

function SessionHead({ session, badge, live, actions }) {
  return (
    <header className="session-head">
      <div>
        <h1>
          {session.title} <span className="badge badge-primary">{badge}</span>
          <span className={`badge ${live ? 'badge-live' : 'badge-gray'}`} title="선생님 질문 · 설정 변경을 바로 받아요">
            <span className={`dot${live ? '' : ' off'}`} /> {live ? '실시간' : '연결 중…'}
          </span>
        </h1>
        <p className="muted">
          {session.topic} · 참가자 {session.participantCount}명 · 코드 <b>{session.inviteCode}</b>
        </p>
      </div>
      {actions && <div className="session-actions">{actions}</div>}
    </header>
  )
}

/** 목업 13 학생 세션 홈. 라이브(화면공유)는 4단계, 공지 · 게시판 · 자료는 5단계 */
function StudentView({ session, reload }) {
  const [tab, setTab] = useState('chat')
  const [keys, setKeys] = useState({ notice: 0, board: 0, materials: 0 })
  const [toast, setToast] = useState('')
  const bump = (k) => setKeys((v) => ({ ...v, [k]: v[k] + 1 }))
  const [quiz, setQuiz] = useState(null)
  const [dismissed, setDismissed] = useState([])
  const [helpKey, setHelpKey] = useState(0)

  // 화면을 열 때 이미 열려 있는 질문이 있으면 띄운다 (늦게 들어온 학생 · 새로고침)
  const loadOpen = useCallback(() => {
    quizzes.open(session.id).then((list) => {
      const latest = list.filter((q) => !q.myAnswer).at(-1)
      if (latest) setQuiz(latest)
    }, () => undefined)
  }, [session.id])
  useEffect(loadOpen, [loadOpen])

  const refreshQuiz = (quizId) => {
    quizzes.get(quizId).then(setQuiz, () => undefined)
  }

  const live = useSessionEvents(session.id, false, (e) => {
    if (e.type === 'settings' || e.type === 'ended') reload()
    else if (e.type === 'quiz') {
      setDismissed((d) => d.filter((x) => x !== e.id))
      refreshQuiz(e.id)
    } else if ((e.type === 'quizResult' || e.type === 'quizClosed') && quiz?.id === e.id) refreshQuiz(e.id)
    else if (e.type === 'helpAccepted' || e.type === 'helpDeclined' || e.type === 'helpEnded') setHelpKey((k) => k + 1)
    else if (e.type === 'notice') {
      bump('notice')
      // 목업 31 알림: 새 공지 제목을 잠깐 띄움
      notices.list(session.id).then((l) => l[0] && setToast(l[0].title), () => undefined)
    } else if (e.type === 'notices') bump('notice')
    else if (e.type === 'posts') bump('board')
    else if (e.type === 'materials') bump('materials')
    else if (e.type === 'reconnect') {
      reload()
      loadOpen()
      setHelpKey((k) => k + 1)
    }
  })

  const showQuiz = quiz && !dismissed.includes(quiz.id) && (quiz.open || quiz.myAnswer)

  return (
    <div className="page session-page">
      <SessionHead session={session} badge="학생" live={live}
        actions={<StudentHelp sessionId={session.id} reloadKey={helpKey} />} />
      {showQuiz && (
        <StudentQuizModal
          key={quiz.id}
          quiz={quiz}
          onAnswered={(a) => setQuiz({ ...quiz, myAnswer: a })}
          onClose={() => setDismissed((d) => [...d, quiz.id])}
        />
      )}
      <nav className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === 'live'} onClick={() => setTab('live')}>라이브</button>
        <button role="tab" aria-selected={tab === 'chat'} onClick={() => setTab('chat')}>챗봇</button>
        <button role="tab" aria-selected={tab === 'notice'} onClick={() => setTab('notice')}>공지</button>
        <button role="tab" aria-selected={tab === 'board'} onClick={() => setTab('board')}>게시판</button>
        <button role="tab" aria-selected={tab === 'materials'} onClick={() => setTab('materials')}>자료</button>
      </nav>
      {tab === 'chat' && <ChatPanel topic={session.topic} sessionId={session.id} />}
      {tab === 'live' && <StudentLiveTab sessionId={session.id} />}
      {tab === 'notice' && <NoticesPanel sessionId={session.id} teacher={false} reloadKey={keys.notice} />}
      {tab === 'board' && <BoardPanel sessionId={session.id} teacher={false} reloadKey={keys.board} />}
      {tab === 'materials' && <MaterialsPanel sessionId={session.id} teacher={false} reloadKey={keys.materials} />}
      {toast && (
        <div className="toast" role="status">
          <span>📢 새 공지: <b>{toast}</b></span>
          <button className="btn btn-sm" onClick={() => { setTab('notice'); setToast('') }}>보기</button>
          <button className="btn-link small" onClick={() => setToast('')} aria-label="닫기">✕</button>
        </div>
      )}
    </div>
  )
}

/** 목업 20 선생님 모드: 참가자 · 주제 설정(목업 21) · 챗봇 미리보기 · 세션 종료 */
function TeacherView({ session, onChange, reload }) {
  const [tab, setTab] = useState('people')
  const [boardKeys, setBoardKeys] = useState({ notice: 0, board: 0, materials: 0 })
  const [peopleKey, setPeopleKey] = useState(0)
  const [quizKey, setQuizKey] = useState(0)
  const [helpKey, setHelpKey] = useState(0)
  const [newHelp, setNewHelp] = useState(false)
  const live = useSessionEvents(session.id, true, (e) => {
    if (e.type === 'joined') reload()   // 참가자 수
    if (e.type === 'joined' || e.type === 'presence') setPeopleKey((k) => k + 1)
    if (e.type === 'quizStats' || e.type === 'quiz' || e.type === 'quizClosed') setQuizKey((k) => k + 1)
    if (e.type === 'notice' || e.type === 'notices') setBoardKeys((v) => ({ ...v, notice: v.notice + 1 }))
    if (e.type === 'posts') setBoardKeys((v) => ({ ...v, board: v.board + 1 }))
    if (e.type === 'materials') setBoardKeys((v) => ({ ...v, materials: v.materials + 1 }))
    if (e.type === 'help') {
      setHelpKey((k) => k + 1)
      setNewHelp(true)
    }
    if (e.type === 'reconnect') {
      setHelpKey((k) => k + 1)
      reload()
      setPeopleKey((k) => k + 1)
      setQuizKey((k) => k + 1)
    }
  })
  return (
    <div className="page session-page">
      <SessionHead session={session} badge="선생님 모드" live={live} />
      <nav className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === 'people'} onClick={() => setTab('people')}>참가자</button>
        <button role="tab" aria-selected={tab === 'settings'} onClick={() => setTab('settings')}>주제 설정</button>
        <button role="tab" aria-selected={tab === 'quiz'} onClick={() => setTab('quiz')}>이해도 질문</button>
        <button role="tab" aria-selected={tab === 'share'} onClick={() => setTab('share')}>화면 공유</button>
        <button role="tab" aria-selected={tab === 'help'} onClick={() => { setTab('help'); setNewHelp(false) }}>
          도움 요청{newHelp && tab !== 'help' && <span className="tab-dot" aria-label="새 요청" />}
        </button>
        <button role="tab" aria-selected={tab === 'materials'} onClick={() => setTab('materials')}>자료 · 공지</button>
        <button role="tab" aria-selected={tab === 'board'} onClick={() => setTab('board')}>게시판</button>
        <button role="tab" aria-selected={tab === 'chat'} onClick={() => setTab('chat')}>챗봇 미리보기</button>
        <EndButton session={session} onChange={onChange} />
      </nav>
      {tab === 'people' && <Participants sessionId={session.id} reloadKey={peopleKey} />}
      {tab === 'quiz' && <TeacherQuizTab sessionId={session.id} reloadKey={quizKey} />}
      {tab === 'share' && <TeacherShareTab sessionId={session.id} />}
      {tab === 'help' && <TeacherHelpTab sessionId={session.id} reloadKey={helpKey} />}
      {tab === 'materials' && (
        <div className="split-2">
          <MaterialsPanel sessionId={session.id} teacher reloadKey={boardKeys.materials} />
          <NoticesPanel sessionId={session.id} teacher reloadKey={boardKeys.notice} />
        </div>
      )}
      {tab === 'board' && <BoardPanel sessionId={session.id} teacher reloadKey={boardKeys.board} />}
      {tab === 'settings' && <Settings session={session} onChange={onChange} />}
      {tab === 'chat' && <ChatPanel topic={session.topic} sessionId={session.id} />}
    </div>
  )
}

function Participants({ sessionId, reloadKey }) {
  const [list, setList] = useState(null)
  const [error, setError] = useState('')
  // 참여 · 접속 변화는 실시간 알림(reloadKey)으로 다시 읽음
  const load = useCallback(() => {
    setError('')
    sessions.participants(sessionId).then(setList, (e) => setError(e instanceof Error ? e.message : '불러오지 못했어요'))
  }, [sessionId])
  useEffect(load, [load, reloadKey])

  return (
    <section className="card session-body">
      <div className="row-between">
        <strong>
          <Icon name="users" size={18} /> 참가자 {list?.length ?? 0}명
          <span className="muted"> · 접속 중 {list?.filter((p) => p.online).length ?? 0}명</span>
        </strong>
        <button className="btn btn-sm" onClick={load}>새로고침</button>
      </div>
      {error && <p className="error-text">{error}</p>}
      <table className="table">
        <thead><tr><th>이름</th><th>접속</th><th>역할</th><th>참여 시각</th></tr></thead>
        <tbody>
          {list?.map((p) => (
            <tr key={p.userId}>
              <td><span className="avatar avatar-sm"><Icon name="user" size={16} /></span> {p.name}</td>
              <td><span className={`dot${p.online ? '' : ' off'}`} aria-label={p.online ? '접속 중' : '접속 안 함'} /></td>
              <td>{p.role === 'TEACHER' ? '선생님' : '학생'}</td>
              <td className="muted">{new Date(p.joinedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}

/** 목업 21: 선생님 설정 → 학생 챗봇의 다음 질문부터 적용 */
function Settings({ session, onChange }) {
  const [form, setForm] = useState({
    topic: session.topic,
    difficulty: session.difficulty,
    hideAnswers: session.hideAnswers,
    preferMaterials: session.preferMaterials,
  })
  const [status, setStatus] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setBusy(true)
    try {
      onChange(await sessions.updateSettings(session.id, form))
      setStatus('학생 챗봇에 적용했어요')
    } catch (err) {
      setStatus(err instanceof Error ? err.message : '저장하지 못했어요')
    } finally {
      setBusy(false)
    }
  }

  const set = (patch) => {
    setForm((f) => ({ ...f, ...patch }))
    setStatus('')
  }

  return (
    <form className="card session-body settings" onSubmit={onSubmit}>
      <div className="field">
        <strong>공부할 과목</strong>
        <SubjectPicker value={form.topic} onChange={(topic) => set({ topic })} />
      </div>
      <label className="setting-row">
        <span className="muted">쉬움</span>
        <input
          type="range" min={1} max={5} value={form.difficulty}
          onChange={(e) => set({ difficulty: Number(e.target.value) })}
          aria-label="설명 난이도"
        />
        <span className="muted">어려움</span>
      </label>
      <label className="setting-row">
        <strong>정답을 바로 알려주지 않기</strong>
        <input type="checkbox" className="switch" checked={form.hideAnswers} onChange={(e) => set({ hideAnswers: e.target.checked })} />
      </label>
      <label className="setting-row">
        <strong>자료 근거 우선 <span className="muted">(자료 업로드 단계부터 동작)</span></strong>
        <input type="checkbox" className="switch" checked={form.preferMaterials} onChange={(e) => set({ preferMaterials: e.target.checked })} />
      </label>
      <div className="row-between">
        <span className="muted" aria-live="polite">{status}</span>
        <button className="btn btn-primary btn-lg" disabled={busy}>학생 챗봇에 적용</button>
      </div>
    </form>
  )
}

function EndButton({ session, onChange }) {
  const navigate = useNavigate()
  const [confirming, setConfirming] = useState(false)

  async function end() {
    onChange(await sessions.end(session.id))
    navigate('/')
  }

  return confirming ? (
    <span className="tabs-end">
      <span className="muted">학생 참여와 챗봇이 막혀요.</span>
      <button className="btn btn-sm btn-danger" onClick={() => void end()}>방 닫기</button>
      <button className="btn btn-sm" onClick={() => setConfirming(false)}>취소</button>
    </span>
  ) : (
    <button className="btn btn-sm tabs-end" onClick={() => setConfirming(true)}>방 닫기…</button>
  )
}
