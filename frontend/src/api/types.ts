// 백엔드 DTO 와 1:1 로 맞추는 타입. 서버 DTO 를 바꾸면 여기도 같이 바꾸기.

/** com.tutipal.domain.user.entity.Role — 계정 역할. 선생님/학생은 세션마다 정해짐 */
export type Role = 'ADMIN' | 'USER'

/** com.tutipal.domain.user.dto.UserResponse */
export interface UserResponse {
  id: number
  name: string
  email: string | null
  provider: SocialProvider | 'dev'
  role: Role
}

export type SocialProvider = 'google' | 'naver' | 'kakao'

/** com.tutipal.domain.tutor.TopicPrompt (소문자로 보냄) */
export type Topic = 'general' | 'coding' | 'english' | 'math'

/** com.tutipal.domain.tutor.dto.ChatRequest.Message */
export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

/** com.tutipal.global.exception.GlobalExceptionHandler 의 응답 */
export interface ApiError {
  status: number
  message: string
}

/** com.tutipal.domain.studysession.entity.SessionRole — 세션 안 역할 */
export type SessionRole = 'TEACHER' | 'STUDENT'

/** com.tutipal.domain.studysession.dto.SessionResponse */
export interface SessionResponse {
  id: number
  title: string
  topic: Topic
  inviteCode: string
  myRole: SessionRole
  difficulty: number
  hideAnswers: boolean
  preferMaterials: boolean
  participantCount: number
  ended: boolean
}

/** com.tutipal.domain.studysession.dto.ParticipantResponse */
export interface ParticipantResponse {
  userId: number
  name: string
  role: SessionRole
  joinedAt: string
  /** 지금 세션 화면을 열어 둔 상태 (STOMP 구독 중) */
  online: boolean
}

/** com.tutipal.domain.studysession.dto.UpdateSettingsRequest */
export interface SessionSettings {
  topic: Topic
  difficulty: number
  hideAnswers: boolean
  preferMaterials: boolean
}

// ---- 이해도 질문: com.tutipal.domain.quiz.dto.QuizDtos ----

export type QuizType = 'CHOICE' | 'TEXT'
/** AUTO 객관식 자동, AI 주관식 AI, TEACHER 선생님. null = 채점 중 */
export type GradedBy = 'AUTO' | 'AI' | 'TEACHER'

export interface QuizDraft {
  type: QuizType
  question: string
  choices: string[] | null
  answerIndex: number | null
  modelAnswer: string | null
}

export interface CreateQuizRequest extends QuizDraft {
  timeLimitSec: number
}

export interface MyAnswer {
  choiceIndex: number | null
  text: string | null
  correct: boolean | null
  gradedBy: GradedBy | null
  feedback: string | null
}

export interface StudentQuiz {
  id: number
  sessionId: number
  type: QuizType
  question: string
  choices: string[] | null
  closesAt: string
  open: boolean
  myAnswer: MyAnswer | null
}

export interface QuizSummary {
  id: number
  no: number
  type: QuizType
  question: string
  choices: string[] | null
  answerIndex: number | null
  modelAnswer: string | null
  closesAt: string
  open: boolean
  answered: number
  correct: number
  pending: number
  studentCount: number
}

export interface AnswerRow {
  answerId: number
  userId: number
  name: string
  choiceIndex: number | null
  text: string | null
  correct: boolean | null
  gradedBy: GradedBy | null
  feedback: string | null
}

/** com.tutipal.global.realtime.SessionEvents.Event — 무엇이 바뀌었는지만, 내용은 REST 로 다시 읽음 */
export interface SessionEvent {
  type: 'quiz' | 'quizClosed' | 'settings' | 'ended' | 'joined' | 'presence' | 'quizStats' | 'quizResult'
    | 'help' | 'helpAccepted' | 'helpDeclined' | 'helpEnded'
    | 'materials' | 'notice' | 'notices' | 'posts'
  id: number
}

// ---- 화면공유: com.tutipal.domain.screenshare.dto.ScreenShareDtos ----

/** LiveKit 접속 정보 (url 로 접속, token 으로 입장) */
export interface JoinInfo {
  url: string
  token: string
  room: string
  identity: string
}

export type HelpStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'DONE'

export interface HelpResponse {
  id: number
  userId: number
  name: string
  message: string
  shareScreen: boolean
  status: HelpStatus
  createdAt: string
}

// ---- 5단계: 자료 · 공지 · 게시판 ----

/** com.tutipal.domain.material.repository.MaterialRepository.MaterialRow */
export interface MaterialRow {
  id: number
  sessionId: number
  filename: string
  sizeBytes: number
  status: 'PROCESSING' | 'DONE' | 'FAILED'
  chunkCount: number
  error: string | null
  createdAt: string
}

/** com.tutipal.domain.notice.dto.NoticeDtos.NoticeResponse */
export interface NoticeResponse {
  id: number
  title: string
  body: string
  authorName: string
  createdAt: string
}

/** com.tutipal.domain.qna.dto.QnaDtos */
export interface PostSummary {
  id: number
  title: string
  authorName: string
  commentCount: number
  createdAt: string
  mine: boolean
}

export interface CommentResponse {
  id: number
  body: string
  authorName: string
  createdAt: string
  mine: boolean
}

export interface PostDetail {
  id: number
  title: string
  body: string
  authorName: string
  createdAt: string
  mine: boolean
  comments: CommentResponse[]
}

// ---- 복습: com.tutipal.domain.review ----

/** ReviewStore.Item */
export interface ReviewItem {
  id: number
  summary: string
  seenCount: number
  seenSeconds: number
  firstSeen: string
  lastSeen: string
  suggestedAt: string | null
}

/** ReviewService.Observed */
export interface ReviewObserved {
  recorded: boolean
  suggestion: { id: number; summary: string } | null
}
