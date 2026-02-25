// view/src/pages/Register.tsx
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom'; // ✅ react-router-dom
import axios from 'axios';

/**
 * OAuth 시작 URL 생성 헬퍼
 */
const buildOAuthUrl = (provider: 'kakao' | 'naver') => {
  const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
  return `${base}/oauth2/authorization/${provider}`;
};

export default function Register() {
  const [formData, setFormData] = useState({ userId: '', email: '', password: '', nickname: '' });
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const res = await axios.post('/auth/register', formData);
      if (res.data.success) {
        navigate('/login');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '회원가입에 실패했습니다.');
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSocialLogin = (provider: 'kakao' | 'naver') => {
    window.location.href = buildOAuthUrl(provider);
  };

  return (
    <div className="max-w-md mx-auto mt-10 bg-white p-8 rounded-xl shadow-sm border border-gray-100">
      <h2 className="text-2xl font-bold text-center text-gray-900 mb-6">회원가입</h2>

      {error && <div className="mb-4 p-3 bg-red-50 text-red-600 text-sm rounded-lg">{error}</div>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">아이디</label>
          <input
            type="text"
            name="userId"
            value={formData.userId}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all"
            required
            pattern="^[a-zA-Z0-9]{4,20}$"
            title="4~20자의 영문/숫자"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
          <input
            type="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all"
            required
            minLength={8}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">닉네임</label>
          <input
            type="text"
            name="nickname"
            value={formData.nickname}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all"
            required
            minLength={2}
            maxLength={20}
          />
        </div>

        <button
          type="submit"
          className="w-full bg-indigo-600 text-white font-medium py-2.5 rounded-lg hover:bg-indigo-700 transition-colors"
        >
          가입하기
        </button>
      </form>

      {/* ✅ 소셜 로그인 버튼 영역 (요청하신 위치: 가입하기 버튼 아래) */}
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
        이미 계정이 있으신가요?{' '}
        <Link to="/login" className="text-indigo-600 hover:underline">
          로그인
        </Link>
      </div>
    </div>
  );
}