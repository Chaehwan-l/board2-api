// view/src/main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router'; 
import App from './App';
import { AuthProvider } from './context/AuthContext';
import './index.css';
import axios from 'axios';

// 환경 변수 주소 연동
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || '';

// 프론트엔드와 백엔드가 도메인(포트)이 달라도 쿠키를 주고받을 수 있게 허용합니다.
axios.defaults.withCredentials = true;

ReactDOM.createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <AuthProvider>
      <App />
    </AuthProvider>
  </BrowserRouter>
);