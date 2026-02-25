package lch.domain.post.dto;

import java.util.List;

// 검색 응답 데이터

public record SearchHistoryResponse(
    List<String> keywords
) {}