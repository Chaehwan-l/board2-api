package lch.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lch.global.common.ApiResponse;

// 모든 Controller에서 발생하는 예외를 여기서 전역으로 가로채어 처리
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 1. 유효성 검증 실패 (@Valid 처리용)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorMessage));
    }

    // 2. 인증 실패 (401)
    @ExceptionHandler(BusinessException.AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthFailed(BusinessException.AuthenticationFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
    }

    // 3. 중복 리소스 (409)
    @ExceptionHandler(BusinessException.DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(BusinessException.DuplicateResourceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
    }

    // 4. 기타 비즈니스 예외 (최상위 비즈니스 예외 처리)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    }

    // 5. 권한 없음 (403)
    @ExceptionHandler(BusinessException.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(BusinessException.AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
    }


}