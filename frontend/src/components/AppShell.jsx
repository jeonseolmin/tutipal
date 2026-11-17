import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'
import Icon, {  } from './Icon'
import Mascot from './Mascot'

const MENU = [
  { to: '/', icon: 'home', label: '홈', ready: true },
  { to: '/chat', icon: 'chat', label: '도우미 챗봇', ready: true },
  { to: '/review', icon: 'book', label: '복습 기록', ready: true },
  { to: '/stats', icon: 'chart', label: '학습 기록 (준비 중)', ready: false },
  { to: '/settings', icon: 'settings', label: '설정', ready: true },
]

/** 목업 공통 틀: 왼쪽 세로 아이콘 메뉴 카드 + 오른쪽 본문 */
export default function AppShell() {
  const { logout } = useAuth()
  return (
    <div className="shell">
      <nav className="side card" aria-label="메인 메뉴">
        <div className="side-logo">
          <Mascot size={44} />
        </div>
        {MENU.map((m) =>
          m.ready ? (
            <NavLink key={m.to} to={m.to} end className="side-item" title={m.label} aria-label={m.label}>
              <Icon name={m.icon} />
            </NavLink>
          ) : (
            <span key={m.to} className="side-item disabled" title={m.label} aria-label={m.label}>
              <Icon name={m.icon} />
            </span>
          ),
        )}
        <button className="side-item side-bottom" onClick={() => void logout()} title="로그아웃" aria-label="로그아웃">
          <Icon name="logout" />
        </button>
      </nav>
      <main className="shell-main">
        <Outlet />
      </main>
    </div>
  )
}
