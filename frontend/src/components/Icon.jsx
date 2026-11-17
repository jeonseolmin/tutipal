/** 화면에 쓰는 선 아이콘 몇 개 (외부 아이콘 라이브러리 없이). */
const PATHS = {
  home: 'M4 11 12 4l8 7v8a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1z',
  chat: 'M5 5h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H10l-5 4v-4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z M8 11h.01 M12 11h.01 M16 11h.01',
  book: 'M3 5c3-1 6-1 9 1 3-2 6-2 9-1v14c-3-1-6-1-9 1-3-2-6-2-9-1z M12 6v14',
  chart: 'M5 20V12 M10 20V8 M15 20V4 M20 20v-6',
  settings: 'M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z M19 12l2-1-1-3-2 .2-1.3-1.3L17 5l-3-1-1 2h-2L10 4 7 5l.3 2L6 8.3 4 8 3 11l2 1v0l-2 1 1 3 2-.2L7.3 17 7 19l3 1 1-2h2l1 2 3-1-.3-2 1.3-1.3 2 .3 1-3z',
  logout: 'M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3 M10 8l-4 4 4 4 M6 12h10',
  plus: 'M12 5v14 M5 12h14',
  users: 'M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z M2.5 20c.5-3.5 3.2-5.5 6.5-5.5s6 2 6.5 5.5 M16 4.5a3.5 3.5 0 0 1 0 6.5 M18 14.8c2 .7 3.3 2.5 3.5 5.2',
  code: 'M8 7l-5 5 5 5 M16 7l5 5-5 5 M14 4l-4 16',
  talk: 'M4 6h16v10H9l-5 4z M9 11h.01 M12 11h.01 M15 11h.01',
  calc: 'M5 4h14v16H5z M8 8h3 M9.5 6.5v3 M13 8h3 M8.5 13l2 2 M10.5 13l-2 2 M13 13.5h3 M13 15.5h3',
  clock: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M12 7v5l3 2',
  send: 'M4 12l16-8-6 16-2-6z',
  pause: 'M9 6v12 M15 6v12',
  play: 'M8 5l11 7-11 7z',
  stop: 'M7 7h10v10H7z',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M4 20c1-4 4-6 8-6s7 2 8 6',
  window: 'M3 5h18v14H3z M3 9h18 M6 7h.01 M9 7h.01',
  shield: 'M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6z M8.5 12l2.5 2.5 4.5-5',
  screen: 'M3 4h18v12H3z M8 20h8 M12 16v4',
  star: 'M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z',
  trash: 'M4 7h16 M9 7V4h6v3 M6 7l1 13h10l1-13 M10 11v6 M14 11v6',
}

export default function Icon({ name, size = 22 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={PATHS[name]} />
    </svg>
  )
}
