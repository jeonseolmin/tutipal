import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { reviewApi } from '../api/review'
import { sessions } from '../api/sessions'
import { useAuth } from '../auth'
import Icon from '../components/Icon'
import Mascot from '../components/Mascot'

const ROOM_CARDS = [
  { to: '/sessions/new', icon: 'plus', label: '방 만들기', desc: '과목을 정하고 초대 코드로 친구 · 학생을 불러요' },
  { to: '/sessions/join', icon: 'users', label: '방 참여하기', desc: '받은 초대 코드를 입력해서 들어가요' },
]

/** 목업 03 홈: 방 만들기 · 방 참여하기 (개인 챗봇은 왼쪽 메뉴) */
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

      <section className="topic-grid room-grid">
        {ROOM_CARDS.map((t) => (
          <Link key={t.to} to={t.to} className="card topic-card">
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
