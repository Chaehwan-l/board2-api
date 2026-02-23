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

    // 1. 비즈니스 로직에서 던진 IllegalArgumentException 처리 (예: 중복 아이디)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409 상태 코드
                .body(ApiResponse.error(e.getMessage()));
    }

    // 2. DTO의 @Valid 검증 실패 시 발생하는 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        // 첫 번째 검증 에러 메시지만 추출하여 반환
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400 상태 코드
                .body(ApiResponse.error(errorMessage));
    }
}