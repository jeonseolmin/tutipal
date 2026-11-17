const { createHash, randomBytes } = require('node:crypto')
const { session, shell } = require('electron')

/**
 * 앱 로그인 (목업 02): 시스템 브라우저에서 소셜 로그인 → 서버가 tutipal://auth?code= 로 돌려보냄 → 여기서 세션 쿠키로 교환.
 * verifier 는 이 프로세스 메모리에만 있고, 서버에는 해시(challenge)만 보낸다 → 딥링크를 가로챈 다른 프로그램은 교환 못 함.
 */
const PROVIDERS = new Set(['google', 'naver', 'kakao', 'dev'])
const PENDING_MS = 5 * 60 * 1000
const b64url = (buf) => buf.toString('base64url')

let pending = null

function startBrowserLogin(base, provider) {
  if (typeof provider !== 'string' || !PROVIDERS.has(provider)) return
  const verifier = b64url(randomBytes(32))
  const challenge = b64url(createHash('sha256').update(verifier).digest())
  pending = { verifier, at: Date.now() }
  const url = new URL('/auth/app/start', base)
  url.searchParams.set('provider', provider)
  url.searchParams.set('challenge', challenge)
  void shell.openExternal(url.href)
}

/** 딥링크 처리. 성공하면 true (메인이 화면을 새로 읽음) */
async function finishBrowserLogin(base, deepLink) {
  let code
  try {
    const u = new URL(deepLink)
    if (u.protocol !== 'tutipal:' || u.hostname !== 'auth') return false
    code = u.searchParams.get('code')
  } catch {
    return false
  }
  // 내가 시작한 로그인이 없거나 오래됐으면 무시 (남이 보낸 딥링크로 다른 계정에 로그인되지 않게)
  if (!code || !/^[A-Za-z0-9_-]{43}$/.test(code) || !pending || Date.now() - pending.at > PENDING_MS) return false
  const { verifier } = pending
  pending = null
  const res = await session.defaultSession.fetch(new URL('/api/auth/app/exchange', base).href, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, verifier }),
    credentials: 'include',
  })
  return res.ok
}

module.exports = { startBrowserLogin, finishBrowserLogin }
