import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { reviewApi } from '../api/review'
import { sessions } from '../api/sessions'
import { useAuth } from '../auth'
import Icon, {  } from '../components/Icon'
import Mascot from '../components/Mascot'

export const TOPICS = [
  { id: 'coding', label: '코딩', icon: 'code', desc: '개념 설명 · 오류 해결' },
  { id: 'english', label: '영어', icon: 'talk', desc: '표현 · 문법 교정' },
  { id: 'math', label: '수학', icon: 'calc', desc: '단계별 풀이' },
]

/** 목업 03 홈: 개인 학습 시작 (주제 선택) */
export default function HomePage() {
  const { me } = useAuth()
  const [mine, setMine] = useState([])
  const [reviewCount, setReviewCount] = useState(0)
  useEffect(() => {
    sessions.mine().then(setMine, () => undefined)
    reviewApi.items().then((l) => setReviewCount(l.length), () => undefined)
  }, [])
  return (
    <div className="page">
      <header className="home-head">
        <Mascot mood="happy" size={130} float />
        <div className="home-greet">
          <p className="muted">{me?.name} 님, 반가워요</p>
          <h1>오늘은 무엇을 공부할까요?</h1>
        </div>
        <div className="home-actions">
          <Link to="/sessions/new" className="btn btn-primary">
            <Icon name="plus" size={18} /> 세션 만들기
          </Link>
          <Link to="/sessions/join" className="btn">
            <Icon name="users" size={18} /> 세션 참여
          </Link>
          {window.desktop && (
            <Link to="/screen-consent" className="btn">
              <Icon name="screen" size={18} /> 화면 분석
            </Link>
          )}
        </div>
      </header>

      {mine.length > 0 && (
        <section className="my-sessions" aria-label="참여 중인 세션">
          {mine.map((s) => (
            <Link key={s.id} to={`/sessions/${s.id}`} className="card my-session">
              <span className="badge badge-primary">{s.myRole === 'TEACHER' ? '선생님' : '학생'}</span>
              <strong>{s.title}</strong>
              <span className="muted">참가자 {s.participantCount}명</span>
            </Link>
          ))}
        </section>
      )}

      <section className="topic-grid">
        {TOPICS.map((t) => (
          <Link key={t.id} to={`/chat?topic=${t.id}`} className="card topic-card">
            <span className="topic-icon"><Icon name={t.icon} size={56} /></span>
            <strong>{t.label}</strong>
            <span className="muted">{t.desc}</span>
          </Link>
        ))}
      </section>

      <Link to="/review" className="card review-strip">
        <span className="review-icon"><Icon name="clock" size={30} /></span>
        <strong>복습할 내용 {reviewCount}개</strong>
        <span className="muted">화면 분석을 켜면 자주 보거나 오래 본 내용이 여기에 모여요</span>
      </Link>
    </div>
  )
}
