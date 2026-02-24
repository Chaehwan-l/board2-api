package lch.domain.post.dto;

public record AttachmentResponse(
    Long id,
    String fileName,
    String s3Key // 프론트엔드에서 S3 URL을 구성하거나 다운로드 API 호출 시 사용
) {}