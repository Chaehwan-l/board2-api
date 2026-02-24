package lch.domain.post.dto;

// Service 계층용 DTO

public record PostCreateCommand(
    Long authorId,
    String title,
    String content
) {}