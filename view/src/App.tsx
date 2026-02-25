/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
// 브라우저 라우팅은 react-router-dom을 사용하는 것이 일반적입니다.
import { Routes, Route, Navigate } from 'react-router-dom';

// AuthProvider는 main.tsx에서 한 번만 감싸도록 하고,
// App.tsx에서는 useAuth만 사용합니다.
import { useAuth } from './context/AuthContext';

import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import PostList from './pages/PostList';
import PostDetail from './pages/PostDetail';
import PostCreate from './pages/PostCreate';
import PostEdit from './pages/PostEdit';

// 보호 라우트: 토큰이 없으면 로그인 페이지로 이동합니다.
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { token } = useAuth();

  // 로그인되지 않은 상태면 로그인 페이지로 리다이렉트
  if (!token) return <Navigate to="/login" replace />;

  // 로그인된 상태면 자식 컴포넌트 렌더링
  return <>{children}</>;
};

export default function App() {
  return (
    // App.tsx에서는 Router/AuthProvider를 다시 감싸지 않고,
    // Routes만 정의합니다.
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<PostList />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />

        {/* 인증 필요한 페이지 */}
        <Route
          path="posts/create"
          element={
            <ProtectedRoute>
              <PostCreate />
            </ProtectedRoute>
          }
        />

        <Route path="posts/:id" element={<PostDetail />} />

        <Route
          path="posts/:id/edit"
          element={
            <ProtectedRoute>
              <PostEdit />
            </ProtectedRoute>
          }
        />
      </Route>
    </Routes>
  );
}