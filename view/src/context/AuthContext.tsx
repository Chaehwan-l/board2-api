// view/src/context/AuthContext.tsx
import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

/**
 * 화면 상단에 표시할 사용자 이름(displayName)을 추가합니다.
 * 현재 백엔드 /auth/me 는 PK만 반환하므로, 우선 fallback 방식으로 동작합니다.
 */
interface AuthContextType {
  token: string | null;
  userPk: string | null;
  displayName: string | null; // ✅ 상단 UI 표시용 이름 (nickname 우선)
  login: (token: string, userPk?: string, displayName?: string) => void;
  logout: () => void;
  refreshMe: () => Promise<void>; // ✅ 필요 시 내 정보 재조회
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [userPk, setUserPk] = useState<string | null>(localStorage.getItem('userPk'));
  const [displayName, setDisplayName] = useState<string | null>(localStorage.getItem('displayName'));

  /**
   * /auth/me 응답을 유연하게 파싱합니다.
   * - 현재: data = number(PK)
   * - 향후(권장): data = { userPk, userId, nickname, ... }
   */
  const refreshMe = async () => {
    if (!token) return;

    try {
      const res = await axios.get('/auth/me', {
        headers: { Authorization: `Bearer ${token}` },
      });

      const data = res?.data?.data;

      // 현재 백엔드: PK만 반환 (number / string)
      if (typeof data === 'number' || typeof data === 'string') {
        const idStr = String(data);
        setUserPk(idStr);
        localStorage.setItem('userPk', idStr);

        // 현재는 nickname 정보가 없으므로 기존 displayName 유지
        // (없으면 userPk를 화면 fallback으로 사용)
        return;
      }

      // 향후 백엔드가 객체 반환하도록 바뀌었을 때 대응 (권장 확장)
      if (data && typeof data === 'object') {
        const nextUserPk = data.userPk ?? data.id ?? data.userIdPk ?? data.pk;
        const nextNickname = data.nickname ?? data.name ?? data.userId;

        if (nextUserPk != null) {
          const idStr = String(nextUserPk);
          setUserPk(idStr);
          localStorage.setItem('userPk', idStr);
        }

        if (nextNickname) {
          const nameStr = String(nextNickname);
          setDisplayName(nameStr);
          localStorage.setItem('displayName', nameStr);
        }
      }
    } catch {
      // 토큰이 만료/유효하지 않으면 로그아웃 처리
      logout();
    }
  };

  useEffect(() => {
    if (token) {
      // 로그인 상태 복원 시 내 정보 재조회
      refreshMe();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  /**
   * 로그인 성공 시 토큰 + (가능하다면) 표시명을 저장합니다.
   * - 로컬 로그인: userPk는 아직 모르므로 비워둬도 됨 (/auth/me에서 채움)
   * - displayName은 로그인 폼 입력값(userId) 등 fallback으로 넣을 수 있음
   */
  const login = (newToken: string, newUserPk: string = '', newDisplayName: string = '') => {
    setToken(newToken);
    localStorage.setItem('token', newToken);

    setUserPk(newUserPk || null);
    if (newUserPk) localStorage.setItem('userPk', newUserPk);
    else localStorage.removeItem('userPk');

    setDisplayName(newDisplayName || null);
    if (newDisplayName) localStorage.setItem('displayName', newDisplayName);
    else localStorage.removeItem('displayName');
  };

  const logout = () => {
    if (token) {
      axios
        .post('/auth/logout', {}, { headers: { Authorization: `Bearer ${token}` } })
        .catch(() => {});
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
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};