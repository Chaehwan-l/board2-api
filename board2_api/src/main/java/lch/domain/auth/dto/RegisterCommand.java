package lch.domain.auth.dto;

// Service 계층용 DTO: 비즈니스 로직을 수행하기 위한 순수한 데이터 전달 객체
// 웹 기술(Validation 어노테이션 등)에 의존하지 않음
public record RegisterCommand(
    String userId,
    String email,
    String password,
    String nickname
) {}