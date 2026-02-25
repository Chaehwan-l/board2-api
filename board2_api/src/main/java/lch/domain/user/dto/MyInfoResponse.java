package lch.domain.user.dto;

// 유저 정보 반환용 Record DTO

public record MyInfoResponse(
    Long userPk,
    String nickname
) {}