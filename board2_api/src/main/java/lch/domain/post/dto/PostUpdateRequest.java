package lch.domain.post.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 웹 계층 dto

public record PostUpdateRequest(
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100)
    String title,

    @NotBlank(message = "내용은 필수입니다.")
    String content,

    // 프론트에서 사용자가 'X' 버튼을 눌러 삭제 처리한 기존 첨부파일의 PK 리스트
    List<Long> deletedAttachmentIds
) {
    public PostUpdateCommand toCommand(List<MultipartFile> newFiles) {
        return new PostUpdateCommand(title, content, deletedAttachmentIds, newFiles);
    }
}