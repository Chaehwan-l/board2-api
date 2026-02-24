package lch.domain.post.dto;

import java.time.LocalDateTime;

/*
 * 게시글 조회 응답을 위한 DTO
 * record를 사용하여 불변(Immutable) 객체로 정의하며, Getter/Setter 없이 필드명으로 데이터에 접근
 */

public record PostResponse(
	    Long id,
	    String title,
	    String content,
	    Long viewCount,
	    String authorNickname,  // 캐시에서 가져올 닉네임 추가
	    LocalDateTime createdAt // 작성일 추가
	) {}