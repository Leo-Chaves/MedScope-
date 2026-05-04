import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    allowedHosts: [
      'localhost',
      '127.0.0.1',
      '192.168.0.103',
      'medscope.loca.lt',
      '.loca.lt'
    ]
  }
})
