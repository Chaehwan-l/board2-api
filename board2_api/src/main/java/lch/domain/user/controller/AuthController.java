package lch.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lch.domain.user.dto.LoginRequest;
import lch.domain.user.dto.MyInfoResponse;
import lch.domain.user.dto.RegisterRequest;
import lch.domain.user.entity.User;
import lch.domain.user.repository.UserRepository;
import lch.domain.user.service.AuthService;
import lch.global.error.ApiResponse;
import lch.global.security.LoginUser;

@Tag(name = "인증/회원 API", description = "회원가입, 로그인 및 유저 정보 관련 API")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "로컬 회원가입", description = "아이디, 이메일, 비밀번호 등을 입력받아 회원가입을 진행합니다.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Long>> register(@Valid @RequestBody RegisterRequest request) {
        // 컨트롤러는 오직 HTTP 요청을 받고, 서비스 로직을 호출한 뒤 응답을 내려주는 것에만 집중
        // 에러 처리는 GlobalExceptionHandler로 완전히 위임됨
        Long savedId = authService.registerLocalUser(request.toCommand());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", savedId));
    }

    @Operation(summary = "로컬 로그인", description = "로그인 성공 시 헤더에 넣을 수 있는 팬텀 토큰(UUID)을 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequest request) {
        // 서비스에서 생성된 팬텀 토큰을 받아옵니다.
        String token = authService.login(request.toCommand());

        return ResponseEntity.ok(
                ApiResponse.success("로그인 성공", token)
        );
    }

    @Operation(summary = "로그아웃", description = "Redis에 저장된 세션(팬텀 토큰)과 검색 기록을 만료시킵니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) @LoginUser Long userId, HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // 서비스의 변경된 파라미터에 맞춰 userId 전달
            authService.logout(token, userId);
        }

        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다.", null));
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 유저의 식별자(PK)와 닉네임을 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(
            @Parameter(hidden = true) @LoginUser Long currentUserId) {

        // 1. SecurityContext에서 꺼낸 PK로 DB에서 실제 유저 정보를 조회합니다.
        // 실무에서는 예외 처리를 GlobalExceptionHandler와 연동된 Custom Exception으로 던지는 것이 좋습니다.
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 조회한 유저 정보로 응답용 DTO를 생성합니다.
        MyInfoResponse responseData = new MyInfoResponse(user.getId(), user.getNickname());

        // 3. 클라이언트에게 반환합니다.
        return ResponseEntity.ok(
                ApiResponse.success("내 정보 조회 성공", responseData)
        );
    }
}
