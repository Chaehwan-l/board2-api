import React from 'react';
import { Outlet, Link, useNavigate } from 'react-router';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, PenSquare } from 'lucide-react';

export default function Layout() {
  const { token, userId, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-gray-50 font-sans text-gray-900">
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <Link to="/" className="text-xl font-bold tracking-tight text-indigo-600">
            게시판
          </Link>
          <nav className="flex items-center gap-4">
            {token ? (
              <>
                <Link to="/posts/create" className="text-sm font-medium text-gray-600 hover:text-indigo-600 flex items-center gap-1">
                  <PenSquare className="w-4 h-4" /> 글쓰기
                </Link>
                <div className="flex items-center gap-2 text-sm font-medium text-gray-600 border-l pl-4 border-gray-200">
                  <User className="w-4 h-4" /> {userId}
                </div>
                <button onClick={handleLogout} className="text-sm font-medium text-gray-600 hover:text-red-600 flex items-center gap-1">
                  <LogOut className="w-4 h-4" /> 로그아웃
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-sm font-medium text-gray-600 hover:text-indigo-600">로그인</Link>
                <Link to="/register" className="text-sm font-medium bg-indigo-600 text-white px-3 py-1.5 rounded-md hover:bg-indigo-700 transition-colors">회원가입</Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  );
}
