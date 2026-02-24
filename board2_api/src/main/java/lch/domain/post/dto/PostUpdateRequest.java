package lch.domain.post.dto;

// 웹 계층 dto

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100)
    String title,

    @NotBlank(message = "내용은 필수입니다.")
    String content
) {
    public PostUpdateCommand toCommand() {
        return new PostUpdateCommand(title, content);
    }
}