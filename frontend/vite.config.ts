import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import {fileURLToPath, URL} from 'url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    watch: {
      // File events don't propagate through Docker bind mounts on Windows/macOS
      usePolling: process.env.VITE_USE_POLLING === 'true',
    },
  },
})
