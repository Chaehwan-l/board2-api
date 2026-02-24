package lch.domain.post.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lch.domain.post.dto.CommentRequest;
import lch.domain.post.dto.PostCreateRequest;
import lch.domain.post.dto.PostListResponse;
import lch.domain.post.dto.PostResponse;
import lch.domain.post.dto.PostUpdateRequest;
import lch.domain.post.dto.SearchHistoryResponse;
import lch.domain.post.service.PostService;
import lch.domain.post.service.SearchService;
import lch.global.error.ApiResponse;
import lch.global.security.LoginUser;

@Tag(name = "게시판 API", description = "게시글 CRUD, 목록 조회 및 검색 API")
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final SearchService searchService;

    // 생성자 주입
    public PostController(PostService postService, SearchService searchService) {
        this.postService = postService;
        this.searchService = searchService;
    }

    @Operation(summary = "게시글 작성 (파일 첨부 포함)", description = "multipart/form-data 형식으로 JSON(request)과 파일 리스트(files)를 받습니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> create(
            @Parameter(hidden = true) @LoginUser Long userId,

            // @RequestBody 대신 @RequestPart를 사용하여 JSON 데이터를 Blob 형태로 받음
            @Valid @RequestPart("request") PostCreateRequest request,

            // 파일 리스트 (필수값이 아니므로 required = false 설정)
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        // DTO의 변경된 메서드에 맞춰 파일 리스트 전달
        Long postId = postService.createPost(request.toCommand(userId, files));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 작성되었습니다.", postId));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글의 상세 내용을 조회하며, 조회수가 1 증가합니다. (Write-back)")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getDetail(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId) {

        PostResponse response = postService.getPost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @Operation(summary = "게시글 삭제", description = "작성자 본인만 게시글을 삭제할 수 있습니다.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId) {

        postService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("삭제 성공", null));
    }

    @Operation(summary = "게시글 목록 페이징 조회", description = "page(0부터 시작)와 size를 입력받아 최신순으로 게시글 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostListResponse>>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 작성일(createdAt) 기준 내림차순(DESC) 정렬 객체 생성
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<PostListResponse> response = postService.getPostList(pageRequest);
        return ResponseEntity.ok(ApiResponse.success("목록 조회 성공", response));
    }

    @Operation(summary = "게시글 수정", description = "작성자 본인만 게시글의 제목, 내용 및 첨부파일을 수정할 수 있습니다.")
    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> update(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId,

            // JSON 데이터를 받기 위해 @RequestBody 대신 @RequestPart 사용
            @Valid @RequestPart("request") PostUpdateRequest request,

            // 새로 추가할 파일들을 받는 파트 (없을 수도 있으므로 required = false)
            @RequestPart(value = "newFiles", required = false) List<MultipartFile> newFiles) {

        Long updatedId = postService.updatePost(postId, userId, request.toCommand(newFiles));
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다.", updatedId));
    }

    @Operation(summary = "최근 검색어 조회", description = "현재 로그인한 유저의 최근 검색어 목록(최대 10개)을 최신순으로 반환합니다.")
    @GetMapping("/search/history")
    public ResponseEntity<ApiResponse<SearchHistoryResponse>> getHistory(
            @Parameter(hidden = true) @LoginUser Long userId) {

        SearchHistoryResponse response = new SearchHistoryResponse(searchService.getHistory(userId));
        return ResponseEntity.ok(ApiResponse.success("검색 기록 조회 성공", response));
    }

    @Operation(summary = "댓글 작성", description = "특정 게시글에 댓글을 작성합니다.")
    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<Long>> createComment(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {

        Long commentId = postService.createComment(postId, userId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글이 작성되었습니다.", commentId));
    }

    @Operation(summary = "댓글 삭제", description = "작성자 본인만 댓글을 삭제할 수 있습니다.")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long commentId) {

        postService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다.", null));
    }

    @Operation(summary = "게시글 키워드 검색", description = "제목이나 내용에 키워드가 포함된 게시글을 검색하고, 최근 검색어에 추가합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PostListResponse>>> searchPosts(
            @Parameter(hidden = true) @LoginUser Long userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostListResponse> response = postService.searchPosts(keyword, userId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("검색 성공", response));
    }

}