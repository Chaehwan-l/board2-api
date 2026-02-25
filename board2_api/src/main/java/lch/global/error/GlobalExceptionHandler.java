package lch.global.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 로거 선언: 실무에서는 System.out 대신 SLF4J 로거를 사용하여 기록을 남깁니다.
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 2. 유효성 검증 실패 (@Valid 처리용)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        // 사용자의 입력 실수이므로 warn 레벨로 로그를 남깁니다.
        log.warn("Validation failed: {}", errorMessage);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorMessage));
    }

    // 3. 인증 실패 (401)
    @ExceptionHandler(BusinessException.AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthFailed(BusinessException.AuthenticationFailedException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
    }

    // 4. 권한 없음 (403)
    @ExceptionHandler(BusinessException.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(BusinessException.AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
    }

    // 5. 중복 리소스 (409)
    @ExceptionHandler(BusinessException.DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(BusinessException.DuplicateResourceException e) {
        log.warn("Duplicate resource: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
    }

    // 6. 기타 정의된 비즈니스 예외 (400)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error("Business error occurred: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    }

    // 7. 예상치 못한 모든 예외 처리 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 서버 개발자가 확인해야 하므로 스택 트레이스를 포함하여 error 레벨로 로그를 남깁니다.
        log.error("Unexpected error occurred: ", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."));
    }
}