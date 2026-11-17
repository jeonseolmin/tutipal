import { request } from './client'

const send = (method, body) => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

/** QuizController */
export const quizzes = {
  // 선생님
  create: (sessionId, req) =>
    request(`/api/sessions/${sessionId}/quizzes`, send('POST', req)),
  generate: (sessionId, type, hint) =>
    request(`/api/sessions/${sessionId}/quizzes/generate`, send('POST', { type, hint })),
  list: (sessionId) => request(`/api/sessions/${sessionId}/quizzes`),
  close: (quizId) => request(`/api/quizzes/${quizId}/close`, { method: 'POST' }),
  answers: (quizId) => request(`/api/quizzes/${quizId}/answers`),
  regrade: (answerId, correct) =>
    request(`/api/quiz-answers/${answerId}`, send('PUT', { correct })),
  // 학생
  open: (sessionId) => request(`/api/sessions/${sessionId}/quizzes/open`),
  get: (quizId) => request(`/api/quizzes/${quizId}`),
  answer: (quizId, body) =>
    request(`/api/quizzes/${quizId}/answers`, send('POST', body)),
}
