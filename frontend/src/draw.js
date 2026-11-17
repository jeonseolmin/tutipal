export const COLORS = ['#ef4444', '#2563eb', '#facc15']
const MAX_POINTS = 5000

/** 받은 메시지는 모양 · 범위를 확인한 뒤에만 쓴다 (데이터 채널 = 신뢰 경계) */
export function parseDraw(raw) {
  try {
    const m = JSON.parse(raw)
    if (m.k === 'clear') return { k: 'clear' }
    if (m.k === 'del' && Array.isArray(m.ids)) return { k: 'del', ids: m.ids.filter((x) => typeof x === 'string').slice(0, 500) }
    if (m.k === 'add' && typeof m.id === 'string' && (m.tool === 'pen' || m.tool === 'hl')
      && typeof m.c === 'string' && (COLORS).includes(m.c) && Array.isArray(m.p)) {
      const p = (m.p).filter((q) => Array.isArray(q) && q.length === 2
        && q.every((v) => typeof v === 'number' && v >= 0 && v <= 1)).slice(0, 500)
      return { k: 'add', id: m.id.slice(0, 40), tool: m.tool, c: m.c, p }
    }
  } catch {
    // 무시
  }
  return null
}

export function applyDraw(strokes, m) {
  if (m.k === 'clear') strokes.clear()
  else if (m.k === 'del') m.ids.forEach((id) => strokes.delete(id))
  else {
    const s = strokes.get(m.id) ?? { tool: m.tool, color: m.c, pts: [] }
    if (s.pts.length + m.p.length <= MAX_POINTS) s.pts.push(...m.p)
    strokes.set(m.id, s)
  }
}

/** 캔버스 크기에 맞춰 다시 그림 */
export function renderDraw(ctx, strokes, w, h) {
  ctx.clearRect(0, 0, w, h)
  const scale = Math.max(1, Math.min(w, h) / 600)
  for (const s of strokes.values()) {
    if (s.pts.length === 0) continue
    ctx.globalAlpha = s.tool === 'hl' ? 0.35 : 1
    ctx.strokeStyle = s.color
    ctx.lineWidth = (s.tool === 'hl' ? 18 : 4) * scale
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.beginPath()
    s.pts.forEach(([x, y], i) => (i === 0 ? ctx.moveTo(x * w, y * h) : ctx.lineTo(x * w, y * h)))
    if (s.pts.length === 1) ctx.lineTo(s.pts[0][0] * w + 0.1, s.pts[0][1] * h)
    ctx.stroke()
  }
  ctx.globalAlpha = 1
}

/** 지우개: 점 근처(비율 r)를 지나는 선 id */
export function hitStrokes(strokes, x, y, r = 0.02) {
  const out = []
  for (const [id, s] of strokes) {
    if (s.pts.some(([px, py]) => Math.hypot(px - x, py - y) < r)) out.push(id)
  }
  return out
}
