import { request } from './client'
import type { AnswerRow, CreateQuizRequest, MyAnswer, QuizDraft, QuizSummary, QuizType, StudentQuiz } from './types'

const send = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

/** QuizController */
export const quizzes = {
  // 선생님
  create: (sessionId: number, req: CreateQuizRequest) =>
    request<QuizSummary>(`/api/sessions/${sessionId}/quizzes`, send('POST', req)),
  generate: (sessionId: number, type: QuizType, hint: string) =>
    request<QuizDraft>(`/api/sessions/${sessionId}/quizzes/generate`, send('POST', { type, hint })),
  list: (sessionId: number) => request<QuizSummary[]>(`/api/sessions/${sessionId}/quizzes`),
  close: (quizId: number) => request<QuizSummary>(`/api/quizzes/${quizId}/close`, { method: 'POST' }),
  answers: (quizId: number) => request<AnswerRow[]>(`/api/quizzes/${quizId}/answers`),
  regrade: (answerId: number, correct: boolean) =>
    request<AnswerRow>(`/api/quiz-answers/${answerId}`, send('PUT', { correct })),
  // 학생
  open: (sessionId: number) => request<StudentQuiz[]>(`/api/sessions/${sessionId}/quizzes/open`),
  get: (quizId: number) => request<StudentQuiz>(`/api/quizzes/${quizId}`),
  answer: (quizId: number, body: { choiceIndex?: number; text?: string }) =>
    request<MyAnswer>(`/api/quizzes/${quizId}/answers`, send('POST', body)),
}
