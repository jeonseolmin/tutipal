import { request } from './client'

const post = (body) => ({
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: body === undefined ? undefined : JSON.stringify(body),
})

/** ScreenShareController: LiveKit 입장권 · 도움 요청 */
export const screenshare = {
  classToken: (sessionId) => request(`/api/sessions/${sessionId}/screenshare/token`, post()),
  requestHelp: (sessionId, message, shareScreen) =>
    request(`/api/sessions/${sessionId}/help`, post({ message, shareScreen })),
  listHelp: (sessionId) => request(`/api/sessions/${sessionId}/help`),
  accept: (helpId) => request(`/api/help/${helpId}/accept`, post()),
  later: (helpId) => request(`/api/help/${helpId}/later`, post()),
  end: (helpId) => request(`/api/help/${helpId}/end`, post()),
  helpToken: (helpId) => request(`/api/help/${helpId}/token`, post()),
}
