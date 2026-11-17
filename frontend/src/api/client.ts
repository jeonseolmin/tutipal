import type { ApiError, UserResponse } from './types'

/** 서버가 4xx/5xx 를 보내면 던지는 오류 (GlobalExceptionHandler 의 {status, message}). */
export class ApiRequestError extends Error {
  readonly status: number

  constructor(error: ApiError) {
    super(error.message)
    this.status = error.status
  }
}

// Spring Security 가 내려주는 XSRF-TOKEN 쿠키 → 변경 요청(POST 등) 헤더로
export function csrfHeader(): Record<string, string> {
  const token = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/)?.[1]
  return token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {}
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (method !== 'GET') {
    for (const [k, v] of Object.entries(csrfHeader())) headers.set(k, v)
  }
  const res = await fetch(path, { ...init, headers })
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as Partial<ApiError> | null
    throw new ApiRequestError({ status: res.status, message: body?.message ?? res.statusText })
  }
  if (res.status === 204 || res.headers.get('content-length') === '0') return undefined as T
  return (await res.json()) as T
}

export const api = {
  /** 로그인 안 했으면 null */
  async me(): Promise<UserResponse | null> {
    try {
      return await request<UserResponse>('/api/users/me')
    } catch (e) {
      if (e instanceof ApiRequestError && e.status === 401) return null
      throw e
    }
  },

  logout: () => request<void>('/logout', { method: 'POST' }),

  /** 로컬 테스트용 로그인 (서버에서 app.dev-login=true 일 때만 동작, 아니면 404) */
  /** n = 계정 번호 1~9 (선생님 · 학생을 따로 시험할 때) */
  devLogin: (n = 1) => request<{ name: string }>(`/dev/login?n=${n}`, { method: 'POST' }),
}

/** 서버 오류 본문({status, message})을 ApiRequestError 로 */
export async function toApiError(res: Response): Promise<ApiRequestError> {
  const body = (await res.json().catch(() => null)) as Partial<ApiError> | null
  return new ApiRequestError({ status: res.status, message: body?.message ?? res.statusText })
}
