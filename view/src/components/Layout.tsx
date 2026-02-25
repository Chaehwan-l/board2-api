import React from 'react';
import { Link, Outlet } from 'react-router';
import { useAuth } from '../context/AuthContext';

export default function Layout() {
  // 상태에서 displayName(닉네임)과 userPk(로그인 여부 판단용)를 가져옵니다.
  const { userPk, displayName, logout } = useAuth();

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-5xl mx-auto px-4 h-16 flex items-center justify-between">
          <Link to="/" className="text-xl font-bold text-gray-900">
            BOARD LOGO
          </Link>
          
          <div className="flex items-center gap-4">
            {/* 토큰이 아니라 userPk가 있으면 로그인 된 것으로 간주합니다 */}
            {userPk ? (
              <div className="flex items-center gap-4">
                <span className="text-sm font-medium text-gray-700">
                  <span className="text-indigo-600 font-bold">{displayName || '회원'}</span>님 환영합니다
                </span>
                <button 
                  onClick={logout} 
                  className="text-sm px-3 py-1.5 border border-gray-300 rounded-md text-gray-600 hover:bg-gray-50 transition-colors"
                >
                  로그아웃
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-3">
                <Link to="/login" className="text-sm font-medium text-gray-600 hover:text-gray-900">
                  로그인
                </Link>
                <Link to="/register" className="text-sm font-medium bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 transition-colors">
                  회원가입
                </Link>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-5xl mx-auto w-full py-8 px-4">
        <Outlet />
      </main>
    </div>
  );
}