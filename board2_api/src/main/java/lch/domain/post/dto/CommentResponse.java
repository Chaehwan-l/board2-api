package lch.domain.post.dto;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    Long authorId,
    String authorNickname,
    String content,
    LocalDateTime createdAt
) {}