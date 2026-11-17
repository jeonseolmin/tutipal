import { request } from './client'

const json = (method, body) => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

/** ReviewController: 내 복습 기록만 (동의한 경우에만 쌓임) */
export const reviewApi = {
  consent: () => request('/api/review/consent'),
  setConsent: (on) => request('/api/review/consent', json('PUT', { on })),
  /** summary 없으면 같은 화면을 계속 보는 중 */
  observe: (summary, seconds) => request('/api/review/observe', json('POST', { summary, seconds })),
  items: () => request('/api/review/items'),
  item: (id) => request(`/api/review/items/${id}`),
  deleteAll: () => request('/api/review/items', { method: 'DELETE' }),
  remove: (id) => request(`/api/review/items/${id}`, { method: 'DELETE' }),
}

/** 챗봇에 보낼 "설명 듣기" 질문 */
export const explainQuestion = (summary) => `내가 자주 본 이 내용을 처음 배우는 사람에게 설명하듯 자세히 알려줘: ${summary}`
