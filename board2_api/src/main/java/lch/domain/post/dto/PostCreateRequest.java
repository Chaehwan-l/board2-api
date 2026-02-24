package lch.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Web 요청용 DTO

public record PostCreateRequest(
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    String title,

    @NotBlank(message = "내용은 필수입니다.")
    String content
) {
    public PostCreateCommand toCommand(Long authorId) {
        return new PostCreateCommand(authorId, title, content);
    }
}