package lch.domain.post.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

// 서비스 계층 dto

public record PostUpdateCommand(
    String title,
    String content,
    List<Long> deletedAttachmentIds,
    List<MultipartFile> newFiles
) {}