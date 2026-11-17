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

/** 목업 03 홈: 맨 위 방 만들기 · 방 참여하기, 아래 내가 방장인 방 · 참여한 방 (각각 스크롤). 개인 챗봇은 왼쪽 메뉴 */
export default function HomePage() {
  const { me } = useAuth()
  const [mine, setMine] = useState(null)
  const [reviewCount, setReviewCount] = useState(0)
  useEffect(() => {
    sessions.mine().then(setMine, () => setMine([]))
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

      <section className="room-grid">
        {ROOM_CARDS.map((t) => (
          <Link key={t.to} to={t.to} className="card room-card">
            <span className="room-icon"><Icon name={t.icon} size={34} /></span>
            <span>
              <strong>{t.label}</strong>
              <span className="muted">{t.desc}</span>
            </span>
          </Link>
        ))}
      </section>

      <section className="room-lists">
        <RoomList
          title="내가 방장인 방"
          rooms={mine?.filter((r) => r.myRole === 'TEACHER')}
          empty="아직 만든 방이 없어요. 위의 방 만들기로 시작해 보세요"
        />
        <RoomList
          title="참여한 방"
          rooms={mine?.filter((r) => r.myRole !== 'TEACHER')}
          empty="아직 참여한 방이 없어요. 초대 코드를 받으면 방 참여하기로 들어가요"
        />
      </section>

      <Link to="/review" className="card review-strip">
        <span className="review-icon"><Icon name="clock" size={30} /></span>
        <strong>복습할 내용 {reviewCount}개</strong>
        <span className="muted">화면 분석을 켜면 자주 보거나 오래 본 내용이 여기에 모여요</span>
      </Link>
    </div>
  )
}

/** 방 목록 한 칸: 길어지면 칸 안에서 스크롤 (닫힌 방은 서버가 빼고 줌) */
function RoomList({ title, rooms, empty }) {
  return (
    <div className="card room-list">
      <h2>{title} {rooms && <span className="badge badge-gray">{rooms.length}</span>}</h2>
      <div className="room-scroll">
        {!rooms && <p className="muted">불러오는 중…</p>}
        {rooms?.length === 0 && <p className="muted">{empty}</p>}
        {rooms?.map((r) => (
          <Link key={r.id} to={`/sessions/${r.id}`} className="room-row">
            <strong>{r.title}</strong>
            <span className="badge">{r.topic}</span>
            <span className="muted small">참가자 {r.participantCount}명</span>
          </Link>
        ))}
      </div>
    </div>
  )
}
