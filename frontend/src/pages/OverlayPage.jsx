import { useEffect, useRef } from 'react'
import { applyDraw, parseDraw, renderDraw } from '../draw'

/**
 * 목업 19: 학생 PC 주 모니터 전체를 덮는 투명 · 클릭 통과 창 (Electron share.ts 의 overlay).
 * 선생님이 그린 선을 받은 그대로 그린다. 이 창은 화면 캡처에서 빠지므로 선생님에게 선이 두 번 보이지 않는다.
 */
export default function OverlayPage() {
  const canvasRef = useRef(null)

  useEffect(() => {
    document.documentElement.classList.add('transparent')
    const strokes = new Map()
    const draw = () => {
      const c = canvasRef.current
      if (!c) return
      c.width = innerWidth
      c.height = innerHeight
      const ctx = c.getContext('2d')
      if (ctx) renderDraw(ctx, strokes, c.width, c.height)
    }
    draw()
    addEventListener('resize', draw)
    const off = window.desktop?.onOverlayDraw((raw) => {
      const m = parseDraw(raw)
      if (!m) return
      applyDraw(strokes, m)
      draw()
    })
    return () => {
      off?.()
      removeEventListener('resize', draw)
      document.documentElement.classList.remove('transparent')
    }
  }, [])

  return (
    <div className="overlay">
      <div className="overlay-banner">((•)) 선생님이 설명 중</div>
      <canvas ref={canvasRef} />
    </div>
  )
}
