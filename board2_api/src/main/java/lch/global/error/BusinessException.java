package lch.global.error;

/*
 * 프로젝트의 모든 비즈니스 예외를 관리하는 최상위 클래스
 */

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    // 로그인 실패 시 발생하는 예외 (401 매핑용)
    public static class AuthenticationFailedException extends BusinessException {
        public AuthenticationFailedException(String message) {
            super(message);
        }
    }

    // 데이터 중복 시 발생하는 예외 (409 매핑용)
    public static class DuplicateResourceException extends BusinessException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    // 권한 없음 시 발생하는 예외 (403 매핑용)
    public static class AccessDeniedException extends BusinessException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }



}