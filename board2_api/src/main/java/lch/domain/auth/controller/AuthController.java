package lch.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lch.domain.auth.dto.RegisterRequest;
import lch.domain.auth.service.AuthService;
import lch.global.common.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Long>> register(@Valid @RequestBody RegisterRequest request) {
        // 컨트롤러는 오직 HTTP 요청을 받고, 서비스 로직을 호출한 뒤 응답을 내려주는 것에만 집중
        // 에러 처리는 GlobalExceptionHandler로 완전히 위임됨
        Long savedId = authService.registerLocalUser(request.toCommand());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", savedId));
    }
}