import { request } from './client'
import type { ParticipantResponse, SessionResponse, SessionSettings, Topic } from './types'

const json = (body: unknown): RequestInit => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

/** StudySessionController (/api/sessions) */
export const sessions = {
  create: (title: string, topic: Topic) => request<SessionResponse>('/api/sessions', json({ title, topic })),
  join: (code: string) => request<SessionResponse>('/api/sessions/join', json({ code })),
  mine: () => request<SessionResponse[]>('/api/sessions'),
  get: (id: number) => request<SessionResponse>(`/api/sessions/${id}`),
  participants: (id: number) => request<ParticipantResponse[]>(`/api/sessions/${id}/participants`),
  updateSettings: (id: number, settings: SessionSettings) =>
    request<SessionResponse>(`/api/sessions/${id}/settings`, { ...json(settings), method: 'PUT' }),
  end: (id: number) => request<SessionResponse>(`/api/sessions/${id}/end`, { method: 'POST' }),
}
