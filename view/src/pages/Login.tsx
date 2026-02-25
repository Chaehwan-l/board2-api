// view/src/pages/Login.tsx
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom'; // ✅ react-router-dom
import axios from 'axios';
import { useAuth } from '../context/AuthContext';

/**
 * OAuth 시작 URL 생성 헬퍼
 * - VITE_API_BASE_URL이 있으면 해당 백엔드 주소 사용
 * - 없으면 현재 origin 기준 상대경로 사용
 */
const buildOAuthUrl = (provider: 'kakao' | 'naver') => {
  const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
  return `${base}/oauth2/authorization/${provider}`;
};

export default function Login() {
  const [userId, setUserId] = useState(''); // 사용자가 폼에 입력하는 로그인용 ID
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const res = await axios.post('/auth/login', { userId, password });

      if (res.data.success) {
        // 현재 백엔드 구조상 로그인 직후 응답 data는 phantom token 문자열
        const phantomToken = res.data.data as string;

        // ✅ displayName fallback으로 userId 저장
        // (현재 /auth/me가 PK만 반환하므로 닉네임을 바로 알 수 없음)
        login(phantomToken, '', userId);

        navigate('/');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '로그인에 실패했습니다.');
    }
  };

  const handleSocialLogin = (provider: 'kakao' | 'naver') => {
    // 백엔드 Spring Security OAuth2 시작 URL로 전체 페이지 이동
    window.location.href = buildOAuthUrl(provider);
  };

  return (
    <div className="max-w-md mx-auto mt-10 bg-white p-8 rounded-xl shadow-sm border border-gray-100">
      <h2 className="text-2xl font-bold text-center text-gray-900 mb-6">로그인</h2>

      {error && <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded-lg">{error}</div>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">아이디</label>
          <input
            type="text"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all"
            required
          />
        </div>

        <button
          type="submit"
          className="w-full bg-indigo-600 text-white font-medium py-2.5 rounded-lg hover:bg-indigo-700 transition-colors"
        >
          로그인
        </button>
      </form>

      {/* ✅ 소셜 로그인 버튼 영역 (요청하신 위치: 로그인 버튼 아래) */}
      <div className="mt-4 space-y-2">
        <button
          type="button"
          onClick={() => handleSocialLogin('kakao')}
          className="w-full bg-yellow-300 text-gray-900 font-medium py-2.5 rounded-lg hover:bg-yellow-400 transition-colors border border-yellow-400"
        >
          카카오로 로그인
        </button>

        <button
          type="button"
          onClick={() => handleSocialLogin('naver')}
          className="w-full bg-green-600 text-white font-medium py-2.5 rounded-lg hover:bg-green-700 transition-colors"
        >
          네이버로 로그인
        </button>
      </div>

      <div className="mt-6 text-center text-sm text-gray-600">
        계정이 없으신가요?{' '}
        <Link to="/register" className="text-indigo-600 hover:underline">
          회원가입
        </Link>
      </div>
    </div>
  );
}