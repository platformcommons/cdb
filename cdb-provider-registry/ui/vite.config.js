import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist'
  },
  server: {
    port: 5173
  },
  // Ensure client-side routing works when served from Spring Boot at '/provider-registry'
  base: '/provider-registry/' 
})
