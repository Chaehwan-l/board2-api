package lch.domain.post.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public record PostCreateCommand(
    Long authorId,
    String title,
    String content,
    List<MultipartFile> files
) {}