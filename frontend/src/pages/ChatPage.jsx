import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { streamChat } from '../api/tutor'
import Icon from '../components/Icon'
import Mascot from '../components/Mascot'
import SubjectPicker from '../components/SubjectPicker'

/** 예전 주소(?topic=coding)도 열리게 id → 과목 이름 */
const LEGACY = { general: '자유 주제', coding: '코딩', english: '영어', math: '수학' }
const DEFAULT_TOPIC = '자유 주제'
/** ChatRequest.Message.content 의 @Size(max) 와 같게 */
const MAX_CONTENT = 8000

const EXAMPLES = {
  '자유 주제': ['오늘 공부 계획 세우는 법 알려줘', '집중이 안 될 때 어떻게 해?'],
  코딩: ['자바에서 static 은 언제 써?', 'NullPointerException 은 왜 나?'],
  영어: ['"I am agree" 맞는 표현이야?', '면접에서 쓸 자기소개 문장 다듬어줘'],
  수학: ['이차방정식 근의 공식 유도해줘', '미분과 적분의 관계가 뭐야?'],
}

/** 개인 챗봇 · 단체 방 모두 과목 이름("한국사") 으로 주고받는다. 서버가 이 이름으로 챗봇 지시문을 만든다 (TopicPrompt) */
const examplesOf = (topic) => EXAMPLES[topic] ?? [`${topic} 기초부터 차근차근 알려줘`, `${topic} 공부 순서를 짜 줘`]

function toTopic(value) {
  return LEGACY[value] ?? (value ?? DEFAULT_TOPIC)
}

/** 목업 06 도우미 챗봇 (개인 학습): 주소의 ?topic= 과목 이름 (드롭다운 · 직접 입력) */
export default function ChatPage() {
  const [params, setParams] = useSearchParams()
  const ask = params.get('ask')
  return (
    <ChatPanel
      topic={toTopic(params.get('topic'))}
      onTopicChange={(t) => setParams({ topic: t }, { replace: true })}
      initialAsk={ask ?? undefined}
      onAsked={() => setParams((p) => { p.delete('ask'); return p }, { replace: true })}
    />
  )
}

/** LLM 이 바로 답하고, 답변이 글자 단위로 흘러나온다. 세션 챗봇(목업 13 챗봇 탭)도 같이 쓴다 */
export function ChatPanel({ topic, onTopicChange, sessionId, initialAsk, onAsked }) {
  const [bubbles, setBubbles] = useState([])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const abortRef = useRef(null)
  const endRef = useRef(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'end' })
  }, [bubbles])

  // 주제 · 세션이 바뀌면 새 대화
  useEffect(() => {
    abortRef.current?.abort()
    setBubbles([])
  }, [topic, sessionId])

  useEffect(() => () => abortRef.current?.abort(), [])

  const asked = useRef(false)
  useEffect(() => {
    if (!initialAsk || asked.current) return
    asked.current = true
    void send(initialAsk.slice(0, MAX_CONTENT))
    onAsked?.()
  })

  async function send(text) {
    const question = text.trim()
    if (!question || streaming) return
    const history = [
      ...bubbles
        .filter((b) => !b.error && b.content)
        // 서버는 한 줄에 8000자까지 받음 → 긴 답변은 앞부분만 기록으로 보냄
        .map(({ role, content }) => ({ role, content: content.slice(0, MAX_CONTENT) })),
      { role: 'user', content: question },
    ]
    setBubbles([...history, { role: 'assistant', content: '' }])
    setInput('')
    setStreaming(true)
    const controller = new AbortController()
    abortRef.current = controller
    try {
      await streamChat(topic.trim() || DEFAULT_TOPIC, history.slice(-20), (delta) => {
        setBubbles((prev) => {
          const next = [...prev]
          const last = next[next.length - 1]
          next[next.length - 1] = { ...last, content: last.content + delta }
          return next
        })
      }, controller.signal, sessionId, (sources) => {
        setBubbles((prev) => {
          const next = [...prev]
          next[next.length - 1] = { ...next[next.length - 1], sources }
          return next
        })
      })
    } catch (e) {
      if (!controller.signal.aborted) {
        setBubbles((prev) => {
          const next = [...prev]
          next[next.length - 1] = {
            role: 'assistant',
            content: e instanceof Error ? e.message : '답변하지 못했어요',
            error: true,
          }
          return next
        })
      }
    } finally {
      setStreaming(false)
    }
  }

  function onSubmit(e) {
    e.preventDefault()
    void send(input)
  }

  function onKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      void send(input)
    }
  }

  const last = bubbles[bubbles.length - 1]

  return (
    <div className="page chat-page card">
      <header className="chat-head">
        <h1>도우미 챗봇</h1>
        {onTopicChange ? (
          <div className="topic-select">
            <span className="muted">주제</span>
            <SubjectPicker value={topic} onChange={onTopicChange} />
          </div>
        ) : (
          <span className="badge">{topic} · 선생님 설정 적용</span>
        )}
      </header>

      <div className="chat-list" aria-live="polite">
        {bubbles.length === 0 && (
          <div className="chat-empty">
            <Mascot mood="curious" size={120} float />
            <p>무엇이든 물어보세요. {topic.trim() || DEFAULT_TOPIC} 주제에 맞춰 답해 드려요.</p>
            <div className="chips">
              {examplesOf(topic.trim() || DEFAULT_TOPIC).map((q) => (
                <button key={q} className="chip" onClick={() => void send(q)}>{q}</button>
              ))}
            </div>
          </div>
        )}
        {bubbles.map((b, i) =>
          b.role === 'user' ? (
            <div key={i} className="msg msg-user">
              <div className="bubble bubble-user">{b.content}</div>
              <span className="avatar"><Icon name="user" /></span>
            </div>
          ) : (
            <div key={i} className="msg msg-ai">
              <span className="avatar avatar-ai">
                <Mascot
                  size={46}
                  mood={b.error ? 'curious' : streaming && b === last ? (b.content ? 'talk' : 'think') : 'idle'}
                />
              </span>
              <div className={`bubble bubble-ai${b.error ? ' bubble-error' : ''}`}>
                {b.content || <span className="typing">답변을 생각하는 중…</span>}
                {b.sources && <span className="sources">📎 참고한 수업 자료: {b.sources.join(', ')}</span>}
              </div>
            </div>
          ),
        )}
        <div ref={endRef} />
      </div>

      <form className="chat-input" onSubmit={onSubmit}>
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder="질문을 입력하세요 (Enter 보내기 · Shift+Enter 줄바꿈)"
          rows={2}
          maxLength={MAX_CONTENT}
        />
        {streaming ? (
          <button type="button" className="btn" onClick={() => abortRef.current?.abort()}>
            <Icon name="stop" size={18} /> 중지
          </button>
        ) : (
          <button type="submit" className="btn btn-primary" disabled={!input.trim()}>
            보내기
          </button>
        )}
      </form>
    </div>
  )
}
