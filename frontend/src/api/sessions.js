import { request } from './client'

const json = (body) => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

/** StudySessionController (/api/sessions) */
export const sessions = {
  create: (title, topic) => request('/api/sessions', json({ title, topic })),
  join: (code) => request('/api/sessions/join', json({ code })),
  mine: () => request('/api/sessions'),
  get: (id) => request(`/api/sessions/${id}`),
  participants: (id) => request(`/api/sessions/${id}/participants`),
  updateSettings: (id, settings) =>
    request(`/api/sessions/${id}/settings`, { ...json(settings), method: 'PUT' }),
  /** 홈 목록 즐겨찾기 (204) */
  pin: (id, pinned) => request(`/api/sessions/${id}/pin`, { ...json({ pinned }), method: 'PUT' }),
  end: (id) => request(`/api/sessions/${id}/end`, { method: 'POST' }),
}
