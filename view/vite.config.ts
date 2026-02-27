// view/vite.config.ts
import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig, loadEnv} from 'vite';

export default defineConfig(({mode}) => {
  const env = loadEnv(mode, '.', '');
  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      port: 3000,
      hmr: process.env.DISABLE_HMR !== 'true',
      // proxy 설정 추가: 프론트엔드에서 /auth, /posts 로 시작하는 요청을 백엔드로 우회시킵니다.
      proxy: {
        '/auth': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/posts': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      }
    },
  };
});