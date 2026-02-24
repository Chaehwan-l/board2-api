package lch.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

// 웹 요청용 DTO

public record LoginRequest(
    @NotBlank(message = "아이디를 입력해주세요.")
    String userId,

    @NotBlank(message = "비밀번호를 입력해주세요.")
    String password
) {
    public LoginCommand toCommand() {
        return new LoginCommand(userId, password);
    }
}