// 백엔드 DTO 와 1:1 로 맞추는 모양 (JSDoc). 서버 DTO 를 바꾸면 여기도 같이 바꾸기.
// 실행에는 영향이 없고, VS Code 가 자동 완성 · 마우스 올리면 설명을 보여 준다. 쓰는 쪽: /** @type {import('./types').SessionResponse} */

/**
 * com.tutipal.domain.user.entity.Role — 계정 역할. 선생님/학생은 세션마다 정해짐
 * @typedef {'ADMIN' | 'USER'} Role
 */

/**
 * com.tutipal.domain.user.dto.UserResponse
 * @typedef {Object} UserResponse
 * @property {number} id
 * @property {string} name
 * @property {string | null} email
 * @property {SocialProvider | 'dev'} provider
 * @property {Role} role
 */

/**
 * @typedef {'google' | 'naver' | 'kakao'} SocialProvider
 */

/**
 * com.tutipal.domain.tutor.TopicPrompt (소문자로 보냄)
 * @typedef {'general' | 'coding' | 'english' | 'math'} Topic
 */

/**
 * com.tutipal.domain.tutor.dto.ChatRequest.Message
 * @typedef {Object} ChatMessage
 * @property {'user' | 'assistant'} role
 * @property {string} content
 */

/**
 * com.tutipal.global.exception.GlobalExceptionHandler 의 응답
 * @typedef {Object} ApiError
 * @property {number} status
 * @property {string} message
 */

/**
 * com.tutipal.domain.studysession.entity.SessionRole — 세션 안 역할
 * @typedef {'TEACHER' | 'STUDENT'} SessionRole
 */

/**
 * com.tutipal.domain.studysession.dto.SessionResponse
 * @typedef {Object} SessionResponse
 * @property {number} id
 * @property {string} title
 * @property {Topic} topic
 * @property {string} inviteCode
 * @property {SessionRole} myRole
 * @property {number} difficulty
 * @property {boolean} hideAnswers
 * @property {boolean} preferMaterials
 * @property {number} participantCount
 * @property {boolean} ended
 */

/**
 * com.tutipal.domain.studysession.dto.ParticipantResponse
 * @typedef {Object} ParticipantResponse
 * @property {number} userId
 * @property {string} name
 * @property {SessionRole} role
 * @property {string} joinedAt
 * @property {boolean} online 지금 세션 화면을 열어 둔 상태 (STOMP 구독 중)
 */

/**
 * com.tutipal.domain.studysession.dto.UpdateSettingsRequest
 * @typedef {Object} SessionSettings
 * @property {Topic} topic
 * @property {number} difficulty
 * @property {boolean} hideAnswers
 * @property {boolean} preferMaterials
 */

/**
 * ---- 이해도 질문: com.tutipal.domain.quiz.dto.QuizDtos ----
 * @typedef {'CHOICE' | 'TEXT'} QuizType
 */

/**
 * AUTO 객관식 자동, AI 주관식 AI, TEACHER 선생님. null = 채점 중
 * @typedef {'AUTO' | 'AI' | 'TEACHER'} GradedBy
 */

/**
 * @typedef {Object} QuizDraft
 * @property {QuizType} type
 * @property {string} question
 * @property {string[] | null} choices
 * @property {number | null} answerIndex
 * @property {string | null} modelAnswer
 */

/**
 * @typedef {QuizDraft & Object} CreateQuizRequest
 * @property {number} timeLimitSec
 */

/**
 * @typedef {Object} MyAnswer
 * @property {number | null} choiceIndex
 * @property {string | null} text
 * @property {boolean | null} correct
 * @property {GradedBy | null} gradedBy
 * @property {string | null} feedback
 */

/**
 * @typedef {Object} StudentQuiz
 * @property {number} id
 * @property {number} sessionId
 * @property {QuizType} type
 * @property {string} question
 * @property {string[] | null} choices
 * @property {string} closesAt
 * @property {boolean} open
 * @property {MyAnswer | null} myAnswer
 */

/**
 * @typedef {Object} QuizSummary
 * @property {number} id
 * @property {number} no
 * @property {QuizType} type
 * @property {string} question
 * @property {string[] | null} choices
 * @property {number | null} answerIndex
 * @property {string | null} modelAnswer
 * @property {string} closesAt
 * @property {boolean} open
 * @property {number} answered
 * @property {number} correct
 * @property {number} pending
 * @property {number} studentCount
 */

/**
 * @typedef {Object} AnswerRow
 * @property {number} answerId
 * @property {number} userId
 * @property {string} name
 * @property {number | null} choiceIndex
 * @property {string | null} text
 * @property {boolean | null} correct
 * @property {GradedBy | null} gradedBy
 * @property {string | null} feedback
 */

/**
 * com.tutipal.global.realtime.SessionEvents.Event — 무엇이 바뀌었는지만, 내용은 REST 로 다시 읽음
 * @typedef {Object} SessionEvent
 * @property {'quiz' | 'quizClosed' | 'settings' | 'ended' | 'joined' | 'presence' | 'quizStats' | 'quizResult' | 'help' | 'helpAccepted' | 'helpDeclined' | 'helpEnded' | 'materials' | 'notice' | 'notices' | 'posts'} type
 * @property {number} id
 */

/**
 * ---- 화면공유: com.tutipal.domain.screenshare.dto.ScreenShareDtos ---- LiveKit 접속 정보 (url 로 접속, token 으로 입장)
 * @typedef {Object} JoinInfo
 * @property {string} url
 * @property {string} token
 * @property {string} room
 * @property {string} identity
 */

/**
 * @typedef {'PENDING' | 'ACCEPTED' | 'DECLINED' | 'DONE'} HelpStatus
 */

/**
 * @typedef {Object} HelpResponse
 * @property {number} id
 * @property {number} userId
 * @property {string} name
 * @property {string} message
 * @property {boolean} shareScreen
 * @property {HelpStatus} status
 * @property {string} createdAt
 */

/**
 * ---- 5단계: 자료 · 공지 · 게시판 ---- com.tutipal.domain.material.repository.MaterialRepository.MaterialRow
 * @typedef {Object} MaterialRow
 * @property {number} id
 * @property {number} sessionId
 * @property {string} filename
 * @property {number} sizeBytes
 * @property {'PROCESSING' | 'DONE' | 'FAILED'} status
 * @property {number} chunkCount
 * @property {string | null} error
 * @property {string} createdAt
 */

/**
 * com.tutipal.domain.notice.dto.NoticeDtos.NoticeResponse
 * @typedef {Object} NoticeResponse
 * @property {number} id
 * @property {string} title
 * @property {string} body
 * @property {string} authorName
 * @property {string} createdAt
 */

/**
 * com.tutipal.domain.qna.dto.QnaDtos
 * @typedef {Object} PostSummary
 * @property {number} id
 * @property {string} title
 * @property {string} authorName
 * @property {number} commentCount
 * @property {string} createdAt
 * @property {boolean} mine
 */

/**
 * @typedef {Object} CommentResponse
 * @property {number} id
 * @property {string} body
 * @property {string} authorName
 * @property {string} createdAt
 * @property {boolean} mine
 */

/**
 * @typedef {Object} PostDetail
 * @property {number} id
 * @property {string} title
 * @property {string} body
 * @property {string} authorName
 * @property {string} createdAt
 * @property {boolean} mine
 * @property {CommentResponse[]} comments
 */

/**
 * ---- 복습: com.tutipal.domain.review ---- ReviewStore.Item
 * @typedef {Object} ReviewItem
 * @property {number} id
 * @property {string} summary
 * @property {number} seenCount
 * @property {number} seenSeconds
 * @property {string} firstSeen
 * @property {string} lastSeen
 * @property {string | null} suggestedAt
 */

/**
 * ReviewService.Observed
 * @typedef {Object} ReviewObserved
 * @property {boolean} recorded
 * @property {{ id: number; summary: string } | null} suggestion
 */

export {}
