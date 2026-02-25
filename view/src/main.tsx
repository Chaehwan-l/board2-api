import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import axios from 'axios';

import './index.css'
import App from './App';
import { AuthProvider } from './context/AuthContext';

// 백엔드 API 주소를 환경변수에서 읽습니다.
// 비어 있으면 상대경로 요청으로 동작합니다.
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || '';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    {/* Router는 앱 전체에서 한 번만 선언해야 합니다. */}
    <BrowserRouter>
      {/* 인증 컨텍스트도 한 번만 감싸도록 합니다. */}
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);