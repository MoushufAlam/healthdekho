import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // the backend's CORS whitelist expects exactly this origin, so never silently move to 5174
    strictPort: true,
  },
})
