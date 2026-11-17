import { request } from './client'

const json = (method, body) => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

/** MaterialController (목업 28) */
export const materials = {
  list: (sessionId) => request(`/api/sessions/${sessionId}/materials`),
  upload: (sessionId, file) => {
    const form = new FormData()
    form.append('file', file)
    return request(`/api/sessions/${sessionId}/materials`, { method: 'POST', body: form })
  },
  remove: (id) => request(`/api/materials/${id}`, { method: 'DELETE' }),
  /** 같은 주소 + 로그인 쿠키라 <a href> 로 바로 내려받기 */
  fileUrl: (id) => `/api/materials/${id}/file`,
}

/** NoticeController (목업 29 왼쪽) */
export const notices = {
  list: (sessionId) => request(`/api/sessions/${sessionId}/notices`),
  create: (sessionId, title, body, alert) =>
    request(`/api/sessions/${sessionId}/notices`, json('POST', { title, body, alert })),
  remove: (id) => request(`/api/notices/${id}`, { method: 'DELETE' }),
}

/** QnaController (목업 29 오른쪽) */
export const posts = {
  list: (sessionId) => request(`/api/sessions/${sessionId}/posts`),
  create: (sessionId, title, body) =>
    request(`/api/sessions/${sessionId}/posts`, json('POST', { title, body })),
  get: (id) => request(`/api/posts/${id}`),
  comment: (id, body) => request(`/api/posts/${id}/comments`, json('POST', { body })),
  remove: (id) => request(`/api/posts/${id}`, { method: 'DELETE' }),
  removeComment: (id) => request(`/api/comments/${id}`, { method: 'DELETE' }),
}
