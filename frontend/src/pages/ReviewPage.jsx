import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { explainQuestion, reviewApi } from '../api/review'
import Icon from '../components/Icon'
import Mascot from '../components/Mascot'

const looksLikeCode = (s) => /코드|프로그램|자바|파이썬|함수|변수|for|class|오류|에러/i.test(s)

/** 목업 10 복습 기록: 자주 · 오래 본 내용 (화면 요약 텍스트만 저장), 설명 듣기 · 기록 삭제 · 동의 켜고 끄기 */
export default function ReviewPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [on, setOn] = useState(null)
  const [items, setItems] = useState([])
  const [confirming, setConfirming] = useState(false)
  const [error, setError] = useState('')

  const explain = useCallback((summary) => {
    navigate(`/chat?ask=${encodeURIComponent(explainQuestion(summary))}`)
  }, [navigate])

  const load = useCallback(() => {
    reviewApi.consent().then((c) => setOn(c.on), () => setOn(false))
    reviewApi.items().then(setItems, (e) => setError(e instanceof Error ? e.message : '불러오지 못했어요'))
  }, [])
  useEffect(load, [load])

  // 위젯의 "설명 듣기"(?explain=id) → 바로 챗봇으로
  useEffect(() => {
    const id = Number(params.get('explain'))
    if (id) reviewApi.item(id).then((it) => explain(it.summary), () => undefined)
  }, [params, explain])

  async function toggle() {
    const next = !on
    await reviewApi.setConsent(next)
    setOn(next)
    if (!next) setItems([])
  }

  async function deleteAll() {
    await reviewApi.deleteAll()
    setItems([])
    setConfirming(false)
  }

  return (
    <div className="page">
      <section className="card session-body review-page">
        <header className="row-between review-head">
          <div className="row-gap">
            <Mascot mood="happy" size={90} />
            <div>
              <h1>복습 기록</h1>
              <p className="muted">화면 분석이 켜져 있을 때 자주 보거나 오래 본 내용이 모여요. 이미지는 저장하지 않고 요약 글만 남아요.</p>
            </div>
          </div>
          {confirming ? (
            <span className="row-gap">
              <span className="muted small">모두 지울까요? 되돌릴 수 없어요</span>
              <button className="btn btn-danger btn-sm" onClick={() => void deleteAll()}>삭제</button>
              <button className="btn btn-sm" onClick={() => setConfirming(false)}>취소</button>
            </span>
          ) : (
            <button className="btn btn-outline-danger" onClick={() => setConfirming(true)} disabled={items.length === 0}>🗑 기록 삭제</button>
          )}
        </header>
        <label className="setting-row">
          <strong>복습 기록 남기기 <span className="muted small">(끄면 지금까지 기록도 지워져요)</span></strong>
          <input type="checkbox" className="switch" checked={!!on} onChange={() => void toggle()} disabled={on === null} />
        </label>
        {error && <p className="error-text">{error}</p>}
        {on === false && <p className="muted">기록이 꺼져 있어요. 켜면 같은 내용을 3번 이상 보거나 10분 넘게 볼 때 캐릭터가 복습을 제안해요.</p>}
        {on && items.length === 0 && <p className="muted">아직 모인 내용이 없어요</p>}
        {items.map((it) => (
          <div key={it.id} className="review-row">
            <span className="post-icon"><Icon name={looksLikeCode(it.summary) ? 'code' : 'book'} size={24} /></span>
            <span className="review-text">{it.summary}</span>
            <span className="badge">{it.seenCount >= 2 ? `${it.seenCount}번 봄` : `${Math.max(1, Math.round(it.seenSeconds / 60))}분 봄`}</span>
            <button className="btn btn-outline" onClick={() => explain(it.summary)}>▶ 설명 듣기</button>
            <button className="btn-link small" onClick={() => void reviewApi.remove(it.id).then(load)} aria-label="이 기록 지우기">✕</button>
          </div>
        ))}
      </section>
    </div>
  )
}
