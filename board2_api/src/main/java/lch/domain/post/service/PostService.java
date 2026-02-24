// lch/domain/post/service/PostService.java
package lch.domain.post.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lch.domain.post.dto.PostCreateCommand;
import lch.domain.post.dto.PostResponse;
import lch.domain.post.entity.Post;
import lch.domain.post.repository.PostRepository;
import lch.domain.user.entity.User;
import lch.domain.user.repository.UserRepository;
import lch.global.error.BusinessException;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RedisViewCountService viewCountService;
    private final SearchService searchService;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       RedisViewCountService viewCountService,
                       SearchService searchService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.viewCountService = viewCountService;
        this.searchService = searchService;
    }

    @Transactional
    public Long createPost(PostCreateCommand command) {
        User author = userRepository.findById(command.authorId())
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        Post post = new Post(author, command.title(), command.content());
        return postRepository.save(post).getId();
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        // 조회수 증가 (Write-back 전략)
        viewCountService.increment(postId);

        return new PostResponse(post.getId(), post.getTitle(), post.getContent(), post.getViewCount());
    }

    // 삭제 로직 (Hard Delete)
    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        // 작성자 본인 확인 로직 필요
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new BusinessException("삭제 권한이 없습니다.");
        }

        postRepository.delete(post);
    }
}