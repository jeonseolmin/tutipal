import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { api } from './api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [me, setMe] = useState(undefined)

  const refresh = useCallback(async () => {
    try {
      setMe(await api.me())
    } catch {
      setMe(null)
    }
  }, [])

  const logout = useCallback(async () => {
    await api.logout().catch(() => undefined)
    setMe(null)
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return <AuthContext.Provider value={{ me, refresh, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const auth = useContext(AuthContext)
  if (!auth) throw new Error('AuthProvider 밖에서 useAuth 사용')
  return auth
}

/** 로그인해야 들어갈 수 있는 화면들을 감싼다 */
export function RequireAuth() {
  const { me } = useAuth()
  if (me === undefined) return <div className="center-page muted">불러오는 중…</div>
  if (me === null) return <Navigate to="/login" replace />
  return <Outlet />
}
