import axios from 'axios';

import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';

import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import PostList from './pages/PostList';
import PostDetail from './pages/PostDetail';
import PostCreate from './pages/PostCreate';
import PostEdit from './pages/PostEdit';

// 1. 브릿지 역할을 할 OAuthRedirect 컴포넌트를 임포트합니다.
import OAuthRedirect from './pages/OAuthRedirect'; 

axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || '';

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { token, userPk } = useAuth(); // OAuth 유저는 token이 없을 수 있으므로 userPk도 함께 확인하는 것이 좋습니다.
  if (!token && !userPk) return <Navigate to="/login" replace />;
  return <>{children}</>;
};

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<PostList />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />

        {/* 2. 백엔드에서 리다이렉트 되는 주소를 React가 낚아챌 수 있도록 라우트를 등록합니다. */}
        <Route path="oauth/redirect" element={<OAuthRedirect />} />

        {/* 인증 필요한 페이지 */}
        <Route path="posts/create" element={<ProtectedRoute><PostCreate /></ProtectedRoute>} />
        <Route path="posts/:id" element={<PostDetail />} />
        <Route path="posts/:id/edit" element={<ProtectedRoute><PostEdit /></ProtectedRoute>} />
      </Route>
    </Routes>
  );
}