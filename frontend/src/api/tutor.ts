import { csrfHeader, toApiError } from './client'
import type { ChatMessage, Topic } from './types'

/**
 * 도우미 챗봇에 묻고 답변을 조각 단위로 받는다 (TutorController 의 SSE).
 * sessionId 를 주면 그 세션 선생님 설정(주제 · 난이도 · 정답 숨김)을 따른다.
 * 이벤트: (세션 자료를 찾았으면) sources {"items"} → delta {"t"} 여러 번 → done, 실패 시 error {"message"}.
 */
export async function streamChat(
  topic: Topic,
  messages: ChatMessage[],
  onDelta: (text: string) => void,
  signal?: AbortSignal,
  sessionId?: number,
  onSources?: (sources: string[]) => void,
): Promise<void> {
  const res = await fetch('/api/tutor/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream', ...csrfHeader() },
    body: JSON.stringify({ topic, messages, sessionId }),
    signal,
  })
  if (!res.ok || !res.body) throw await toApiError(res)

  const reader = res.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    // done/error 이벤트 없이 끝났다면 중간에 끊긴 것 (서버 재시작 · 프록시 타임아웃 등)
    if (done) throw new Error('연결이 끊겨 답변이 중간에 멈췄어요')
    buffer += value.replace(/\r\n/g, '\n')
    let end: number
    while ((end = buffer.indexOf('\n\n')) >= 0) {
      const raw = buffer.slice(0, end)
      buffer = buffer.slice(end + 2)
      const event = parseEvent(raw)
      if (event.name === 'delta') onDelta((JSON.parse(event.data) as { t: string }).t)
      else if (event.name === 'sources') onSources?.((JSON.parse(event.data) as { items: string[] }).items)
      else if (event.name === 'error') throw new Error((JSON.parse(event.data) as { message: string }).message)
      else if (event.name === 'done') return
    }
  }
}

function parseEvent(raw: string): { name: string; data: string } {
  let name = 'message'
  const data: string[] = []
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) name = line.slice(6).trim()
    else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
  }
  return { name, data: data.join('\n') }
}
