// lch/domain/post/controller/PostController.java
package lch.domain.post.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lch.domain.post.dto.PostCreateRequest;
import lch.domain.post.dto.PostResponse;
import lch.domain.post.service.PostService;
import lch.global.common.ApiResponse;
import lch.global.security.annotation.LoginUser;

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

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(hidden = true) @LoginUser Long userId,
            @PathVariable Long postId) {

        postService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("삭제 성공", null));
    }
}