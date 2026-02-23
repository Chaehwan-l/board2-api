package lch.domain.auth.dto;

// 비지니스 서비스용 DTO

public record LoginCommand(
    String userId,
    String password
) {}