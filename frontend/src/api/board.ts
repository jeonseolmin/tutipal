import { request } from './client'
import type { MaterialRow, NoticeResponse, PostDetail, PostSummary } from './types'

const json = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

/** MaterialController (목업 28) */
export const materials = {
  list: (sessionId: number) => request<MaterialRow[]>(`/api/sessions/${sessionId}/materials`),
  upload: (sessionId: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<MaterialRow>(`/api/sessions/${sessionId}/materials`, { method: 'POST', body: form })
  },
  remove: (id: number) => request<void>(`/api/materials/${id}`, { method: 'DELETE' }),
  /** 같은 주소 + 로그인 쿠키라 <a href> 로 바로 내려받기 */
  fileUrl: (id: number) => `/api/materials/${id}/file`,
}

/** NoticeController (목업 29 왼쪽) */
export const notices = {
  list: (sessionId: number) => request<NoticeResponse[]>(`/api/sessions/${sessionId}/notices`),
  create: (sessionId: number, title: string, body: string, alert: boolean) =>
    request<NoticeResponse>(`/api/sessions/${sessionId}/notices`, json('POST', { title, body, alert })),
  remove: (id: number) => request<void>(`/api/notices/${id}`, { method: 'DELETE' }),
}

/** QnaController (목업 29 오른쪽) */
export const posts = {
  list: (sessionId: number) => request<PostSummary[]>(`/api/sessions/${sessionId}/posts`),
  create: (sessionId: number, title: string, body: string) =>
    request<PostSummary>(`/api/sessions/${sessionId}/posts`, json('POST', { title, body })),
  get: (id: number) => request<PostDetail>(`/api/posts/${id}`),
  comment: (id: number, body: string) => request<PostDetail>(`/api/posts/${id}/comments`, json('POST', { body })),
  remove: (id: number) => request<void>(`/api/posts/${id}`, { method: 'DELETE' }),
  removeComment: (id: number) => request<void>(`/api/comments/${id}`, { method: 'DELETE' }),
}
