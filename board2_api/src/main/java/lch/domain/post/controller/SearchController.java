package lch.domain.post.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import lch.domain.post.dto.SearchHistoryResponse;
import lch.domain.post.service.SearchService;
import lch.global.common.ApiResponse;
import lch.global.security.LoginUser;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    // 최근 검색어 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<SearchHistoryResponse>> getHistory(
            @Parameter(hidden = true) @LoginUser Long userId) {

        SearchHistoryResponse response = new SearchHistoryResponse(searchService.getHistory(userId));
        return ResponseEntity.ok(ApiResponse.success("검색 기록 조회 성공", response));
    }
}