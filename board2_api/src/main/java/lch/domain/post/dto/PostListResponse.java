package lch.domain.post.dto;

import java.time.LocalDateTime;

// 목록 조회 응답용 dto
// 목록에서는 본문(content) 전체가 필요 없으므로 성능 최적화를 위해 제외

public record PostListResponse(
    Long id,
    String title,
    String authorNickname,
    Long viewCount,
    LocalDateTime createdAt
) {}