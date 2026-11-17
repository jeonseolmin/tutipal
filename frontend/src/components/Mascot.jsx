import sheet from '../assets/sage-sprites.webp'

/**
 * 학습 도우미 캐릭터 "Sage" (파란 부엉이, AI 안경 · 태블릿). 원본: 팀 디자인 sage-ai-glasses 스프라이트 시트.
 * 시트는 칸 192×208, 가로 8칸 × 세로 11줄. 줄마다 동작 하나 (frames = 그 줄에 그려진 칸 수).
 */
const CELL_W = 192
const CELL_H = 208
const ROWS = {
  idle: { row: 0, frames: 6, ms: 220 },
  waving: { row: 3, frames: 4, ms: 180 },
  jumping: { row: 4, frames: 5, ms: 140 },
  failed: { row: 5, frames: 8, ms: 260 },
  waiting: { row: 6, frames: 6, ms: 200 },
  thinking: { row: 7, frames: 6, ms: 240 }, // 시트 이름은 running (작업 · 처리 중), 생각하는 모습으로 씀
  review: { row: 8, frames: 6, ms: 220 },
  look: { row: 9, frames: 8, ms: 260 },
}
/** 화면에서 쓰는 기분 이름 → 시트의 동작 */
const MOOD = {
  idle: 'idle',
  happy: 'waving',
  talk: 'waiting',
  think: 'thinking',
  curious: 'look',
  alert: 'jumping',
  sleep: 'failed',
  listen: 'review',
}
/** 칸 안에서 부엉이가 있는 영역 (모든 칸 bbox 합): 가로 16~176, 세로 44~208 → size 는 이 160px 폭 기준 */
const VIEW_X = 16
const VIEW_Y = 44
const VIEW_W = 160

export default function Mascot({ mood = 'idle', size = 120, float = false }) {
  const a = ROWS[MOOD[mood] ?? 'idle']
  const k = size / VIEW_W
  return (
    <span className={float ? 'sage-box float' : 'sage-box'} style={{ width: size, height: (CELL_H - VIEW_Y) * k }}>
      <span
        className="sage"
        role="img"
        aria-label="학습 도우미 캐릭터 Sage"
        style={{
          '--k': k,
          '--n': a.frames,
          width: CELL_W * k,
          height: CELL_H * k,
          // 점프 프레임이 칸 위쪽까지 올라가므로 잘라내지 않고 위로 넘치게 둔다
          left: -VIEW_X * k,
          top: -VIEW_Y * k,
          backgroundImage: `url(${sheet})`,
          backgroundPositionY: -a.row * CELL_H * k,
          animationDuration: `${a.frames * a.ms}ms`,
        }}
      />
    </span>
  )
}
