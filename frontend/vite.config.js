import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// npm run dev (5173) → 백엔드(8080)로 프록시. 배포 땐 nginx 가 같은 역할
const backend = 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backend,
      '/oauth2': backend,
      '/login/oauth2': backend,
      '/logout': backend,
      '/ws': { target: backend, ws: true },
    },
  },
})
