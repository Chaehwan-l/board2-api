import React, { useEffect } from 'react';
import { useNavigate } from 'react-router'; 
import { useAuth } from '../context/AuthContext';

export default function OAuthRedirect() {
  const navigate = useNavigate();
  const { refreshMe } = useAuth();

  useEffect(() => {
    const initOAuth = async () => {
      // 1. AuthContext의 refreshMe()를 호출합니다.
      // (이때 브라우저가 알아서 백엔드가 구워준 access_token 쿠키를 함께 전송합니다.)
      const success = await refreshMe();

      // 2. 백엔드 필터가 쿠키를 읽고 성공 응답을 내렸다면 메인 화면으로 이동합니다.
      if (success) {
        navigate('/', { replace: true });
      } else {
        alert('소셜 로그인에 실패했거나 세션이 만료되었습니다.');
        navigate('/login', { replace: true });
      }
    };

    initOAuth();
  }, [refreshMe, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-gray-600 font-medium text-lg">
        보안 로그인 처리 중입니다...
      </div>
    </div>
  );
}