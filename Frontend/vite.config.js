import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // `npm run dev`: forward API calls to the Spring Boot backend. The Host header is kept
    // (changeOrigin: false; the string shorthand would set it to true), so the backend sees
    // the same origin the browser sent and needs no CORS entry for localhost.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: false },
    },
  },
})
