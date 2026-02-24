// lch/domain/post/controller/PostController.java
package lch.domain.post.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lch.domain.post.dto.PostCreateRequest;
import lch.domain.post.dto.PostListResponse;
import lch.domain.post.dto.PostResponse;
import lch.domain.post.dto.PostUpdateRequest;
import lch.domain.post.service.PostService;
import lch.global.common.ApiResponse;
import lch.global.security.LoginUser;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Valid @RequestBody PostCreateRequest request) {

        Long postId = postService.createPost(request.toCommand(userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 작성되었습니다.", postId));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getDetail(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId) {

        PostResponse response = postService.getPost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    // 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId) {

        postService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("삭제 성공", null));
    }

    // 게시글 목록 페이징 조회 (최신순 정렬 기본값)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostListResponse>>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 작성일(createdAt) 기준 내림차순(DESC) 정렬 객체 생성
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<PostListResponse> response = postService.getPostList(pageRequest);
        return ResponseEntity.ok(ApiResponse.success("목록 조회 성공", response));
    }

    // 게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<Long>> update(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request) {

        Long updatedId = postService.updatePost(postId, userId, request.toCommand());
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다.", updatedId));
    }




}