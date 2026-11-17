const INK = '#5b6472'
const BLUE = '#1a73f0'
const CHEEK = '#c9dcff'

export default function Mascot({ mood = 'idle', size = 120, float = false }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 120 120"
      role="img"
      aria-label="학습 도우미 캐릭터"
      className={float ? 'float' : undefined}
    >
      {/* 그림자 */}
      <ellipse cx="60" cy="110" rx="30" ry="5" fill="#dbe7fb" />
      {/* 안테나 */}
      <line x1="60" y1="30" x2="60" y2="17" stroke={INK} strokeWidth="3" strokeLinecap="round" />
      <circle cx="60" cy="13" r="6" fill={BLUE} />
      {/* 팔 */}
      <path d="M30 74 q-12 -2 -13 -12" fill="none" stroke={INK} strokeWidth="3" strokeLinecap="round" />
      <path d="M90 74 q12 -2 13 -12" fill="none" stroke={INK} strokeWidth="3" strokeLinecap="round" />
      {/* 몸 */}
      <path
        d="M60 28 C83 28 95 46 95 68 C95 92 82 106 60 106 C38 106 25 92 25 68 C25 46 37 28 60 28 Z"
        fill="#fff"
        stroke={INK}
        strokeWidth="3"
      />
      {/* 목도리 */}
      <path d="M29 70 Q60 84 91 70 L92 78 Q60 93 28 78 Z" fill={BLUE} />
      <path d="M78 80 L86 96 L94 90 L86 76 Z" fill={BLUE} />
      {/* 볼 */}
      <ellipse cx="40" cy="58" rx="6" ry="4.5" fill={CHEEK} />
      <ellipse cx="80" cy="58" rx="6" ry="4.5" fill={CHEEK} />
      <Face mood={mood} />
      <Extra mood={mood} />
    </svg>
  )
}

function Face({ mood }) {
  const eye = (cx, cy = 50, r = 3.6) => <circle cx={cx} cy={cy} r={r} fill={INK} />
  const stroke = { fill: 'none', stroke: INK, strokeWidth: 2.6, strokeLinecap: 'round' }
  switch (mood) {
    case 'happy':
      return (
        <g>
          <path d="M44 51 q4 -5 8 0" {...stroke} />
          <path d="M68 51 q4 -5 8 0" {...stroke} />
          <path d="M53 58 q7 8 14 0 z" fill={INK} />
        </g>
      )
    case 'talk':
      return (
        <g>
          {eye(48)}
          {eye(72)}
          <path d="M54 57 q6 9 12 0 z" fill="#e8596b" stroke={INK} strokeWidth="1.5" />
        </g>
      )
    case 'think':
      return (
        <g>
          {eye(50, 46)}
          {eye(74, 46)}
          <path d="M55 60 h10" {...stroke} />
        </g>
      )
    case 'curious':
      return (
        <g>
          {eye(48, 50, 3)}
          {eye(72, 49, 4.4)}
          <circle cx="60" cy="60" r="3" fill="none" stroke={INK} strokeWidth="2.4" />
        </g>
      )
    case 'alert':
      return (
        <g>
          {eye(48, 50, 4.6)}
          {eye(72, 50, 4.6)}
          <ellipse cx="60" cy="61" rx="3.5" ry="4.5" fill={INK} />
        </g>
      )
    case 'sleep':
      return (
        <g>
          <path d="M43 51 q5 4 10 0" {...stroke} />
          <path d="M67 51 q5 4 10 0" {...stroke} />
          <path d="M56 60 q4 2 8 0" {...stroke} />
        </g>
      )
    default: // idle · listen
      return (
        <g>
          {eye(48)}
          {eye(72)}
          <path d="M54 58 q6 6 12 0" {...stroke} />
        </g>
      )
  }
}

function Extra({ mood }) {
  const spark = { stroke: BLUE, strokeWidth: 3, strokeLinecap: 'round' }
  switch (mood) {
    case 'happy':
    case 'talk':
      return (
        <g>
          <line x1="88" y1="26" x2="92" y2="19" {...spark} />
          <line x1="94" y1="31" x2="101" y2="27" {...spark} />
          <line x1="82" y1="22" x2="83" y2="15" {...spark} />
        </g>
      )
    case 'think':
      return (
        <g fill={BLUE}>
          <circle cx="84" cy="24" r="2.5" />
          <circle cx="92" cy="19" r="3" />
          <circle cx="101" cy="13" r="3.5" />
        </g>
      )
    case 'curious':
      return <text x="86" y="30" fontSize="22" fontWeight="800" fill={BLUE}>?</text>
    case 'alert':
      return <text x="88" y="30" fontSize="22" fontWeight="800" fill={BLUE}>!</text>
    case 'sleep':
      return <text x="84" y="28" fontSize="16" fontWeight="800" fill={BLUE}>z</text>
    case 'listen':
      return (
        <g fill="none" stroke={BLUE} strokeWidth="2.5" strokeLinecap="round">
          <path d="M98 44 q5 8 0 16" />
          <path d="M104 39 q9 13 0 26" />
        </g>
      )
    default:
      return null
  }
}
