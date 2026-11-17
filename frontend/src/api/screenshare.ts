import { request } from './client'
import type { HelpResponse, JoinInfo } from './types'

const post = (body?: unknown): RequestInit => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: body === undefined ? undefined : JSON.stringify(body),
})

/** ScreenShareController: LiveKit 입장권 · 도움 요청 */
export const screenshare = {
  classToken: (sessionId: number) => request<JoinInfo>(`/api/sessions/${sessionId}/screenshare/token`, post()),
  requestHelp: (sessionId: number, message: string, shareScreen: boolean) =>
    request<HelpResponse>(`/api/sessions/${sessionId}/help`, post({ message, shareScreen })),
  listHelp: (sessionId: number) => request<HelpResponse[]>(`/api/sessions/${sessionId}/help`),
  accept: (helpId: number) => request<HelpResponse>(`/api/help/${helpId}/accept`, post()),
  later: (helpId: number) => request<void>(`/api/help/${helpId}/later`, post()),
  end: (helpId: number) => request<void>(`/api/help/${helpId}/end`, post()),
  helpToken: (helpId: number) => request<JoinInfo>(`/api/help/${helpId}/token`, post()),
}
