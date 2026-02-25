// view/src/context/AuthContext.tsx
import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import axios from 'axios';

interface AuthContextType {
  token: string | null;
  userPk: string | null;
  displayName: string | null;
  login: (token: string | null, userPk?: string, displayName?: string) => void;
  logout: () => void;
  refreshMe: () => Promise<boolean>; // 반환값을 boolean으로 두어 성공 여부 파악
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [userPk, setUserPk] = useState<string | null>(localStorage.getItem('userPk'));
  const [displayName, setDisplayName] = useState<string | null>(localStorage.getItem('displayName'));

  // 백엔드의 /auth/me 를 호출하여 내 정보(PK, 닉네임)를 갱신합니다.
  const refreshMe = useCallback(async () => {
    try {
      // 로컬 로그인 유저는 token을 헤더에 넣고, OAuth 유저는 브라우저 쿠키가 알아서 날아갑니다.
      const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
      const res = await axios.get('/auth/me', config);

      const data = res?.data?.data;
      
      // 백엔드에서 만든 MyInfoResponse(Record) 형식: { userPk: 1, nickname: "홍길동" }
      if (data && typeof data === 'object') {
        const nextUserPk = String(data.userPk);
        const nextNickname = data.nickname;

        setUserPk(nextUserPk);
        setDisplayName(nextNickname);
        localStorage.setItem('userPk', nextUserPk);
        localStorage.setItem('displayName', nextNickname);
        
        return true; // 로그인/인증 성공
      }
      return false;
    } catch {
      // 쿠키도 만료되었고 토큰도 유효하지 않은 경우
      logout();
      return false;
    }
  }, [token]);

  useEffect(() => {
    // 앱이 처음 로드될 때, 토큰이 있거나(로컬로그인) 토큰은 없지만 userPk가 남아있다면(OAuth) 세션 검증 시도
    if (token || userPk) {
      refreshMe();
    }
  }, [token, userPk, refreshMe]);

  const login = (newToken: string | null, newUserPk: string = '', newDisplayName: string = '') => {
    if (newToken) {
      setToken(newToken);
      localStorage.setItem('token', newToken);
    }
    // OAuth의 경우 newToken은 null로 들어오고, 이후 refreshMe()가 호출되며 채워집니다.
  };

  const logout = () => {
    if (token) {
      axios.post('/auth/logout', {}, { headers: { Authorization: `Bearer ${token}` } }).catch(() => {});
    }
    setToken(null);
    setUserPk(null);
    setDisplayName(null);
    localStorage.removeItem('token');
    localStorage.removeItem('userPk');
    localStorage.removeItem('displayName');
  };

  return (
    <AuthContext.Provider value={{ token, userPk, displayName, login, logout, refreshMe }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) throw new Error('useAuth error');
  return context;
};