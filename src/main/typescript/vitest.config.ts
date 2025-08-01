import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./test-setup.ts'],
    env: {
      MODE: 'test',
      DEV: 'false',
      PROD: 'false',
      SSR: 'false'
    }
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './internal')
    }
  },
  define: {
    'import.meta.env': JSON.stringify({
      MODE: 'test',
      DEV: false,
      PROD: false,
      SSR: false
    }),
    'import.meta.env.MODE': '"test"',
    'import.meta.env.DEV': 'false',
    'import.meta.env.PROD': 'false',
    'import.meta.env.SSR': 'false'
  }
})
