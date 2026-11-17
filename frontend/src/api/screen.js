import { request } from './client'

/** ScreenController: 화면 요약(question 없음) · 화면 질문. 이미지는 서버에도 저장되지 않는다 */
export const screenApi = {
  describe: (image, question) =>
    request('/api/screen', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ image, question }),
    }),
}
