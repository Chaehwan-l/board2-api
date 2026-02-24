package lch.domain.post.dto;

/*
 * 게시글 조회 응답을 위한 DTO
 * record를 사용하여 불변(Immutable) 객체로 정의하며, Getter/Setter 없이 필드명으로 데이터에 접근
 */

public record PostResponse(
	    Long id,
	    String title,
	    String content,
	    Long viewCount
	) {}