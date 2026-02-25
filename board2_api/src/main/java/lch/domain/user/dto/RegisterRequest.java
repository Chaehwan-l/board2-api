package lch.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Web 계층용 DTO: 컨트롤러에서 HTTP 요청을 받고 검증(@Valid)하는 역할만 수행
public record RegisterRequest(
    @NotBlank(message = "아이디는 필수입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 영문, 숫자 조합 4~20자리여야 합니다.")
    String userId,

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    String password,

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하이어야 합니다.")
    String nickname
) {
    // Web DTO를 Service 계층 전용 DTO(Command)로 변환
    public RegisterCommand toCommand() {
        return new RegisterCommand(userId, email, password, nickname);
    }
}