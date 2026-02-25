 package lch.domain.post.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    String title,

    @NotBlank(message = "내용은 필수입니다.")
    String content
) {
    // Controller에서 받은 files를 Command로 함께 넘겨줍니다.
    public PostCreateCommand toCommand(Long authorId, List<MultipartFile> files) {
        return new PostCreateCommand(authorId, title, content, files);
    }
}