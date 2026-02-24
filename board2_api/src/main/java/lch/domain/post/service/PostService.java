// lch/domain/post/service/PostService.java
package lch.domain.post.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lch.domain.post.dto.PostCreateCommand;
import lch.domain.post.dto.PostListResponse;
import lch.domain.post.dto.PostResponse;
import lch.domain.post.dto.PostUpdateCommand;
import lch.domain.post.entity.Post;
import lch.domain.post.repository.PostRepository;
import lch.domain.user.entity.User;
import lch.domain.user.repository.UserRepository;
import lch.domain.user.service.UserCacheService;
import lch.global.error.BusinessException;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RedisViewCountService viewCountService;
    private final SearchService searchService;
    private final UserCacheService userCacheService;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       RedisViewCountService viewCountService,
                       SearchService searchService,
                       UserCacheService userCacheService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.viewCountService = viewCountService;
        this.searchService = searchService;
        this.userCacheService = userCacheService;
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

        // 조회수 Write-back
        viewCountService.increment(postId);

        // 작성자 정보 Cache-aside 연동
        String authorNickname = userCacheService.getUserNickname(post.getAuthor().getId());

        // 확장된 DTO로 반환 (post의 필드 접근을 위해 엔티티에 getCreatedAt() 등 필요)
        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getViewCount(),
            authorNickname,
            post.getCreatedAt()
        );
    }

    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        // 403 에러로 처리
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new BusinessException.AccessDeniedException("게시글 삭제 권한이 없습니다.");
        }

        postRepository.delete(post);
    }


 // 1. 게시글 수정 (Update)
    @Transactional
    public Long updatePost(Long postId, Long currentUserId, PostUpdateCommand command) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new BusinessException.AccessDeniedException("게시글 수정 권한이 없습니다.");
        }

        // JPA의 더티 체킹(Dirty Checking)으로 인해 save() 호출 불필요
        post.update(command.title(), command.content());
        return post.getId();
    }

    // 2. 게시글 목록 페이징 조회 (Read - List)
    @Transactional(readOnly = true)
    public Page<PostListResponse> getPostList(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);

        return posts.map(post -> {
            // DB의 조회수와 Redis에 임시 저장된(아직 스케줄러가 퍼가지 않은) 조회수를 합산하여 보여줌
            // Write-back 환경에서 사용자에게 실시간 조회수를 제공하기 위한 필수 로직
            Long redisCount = viewCountService.getCount("post:view:count:" + post.getId());
            Long totalViewCount = post.getViewCount() + redisCount;

            // Cache-aside: 유저 닉네임을 캐시에서 우선 조회
            String nickname = userCacheService.getUserNickname(post.getAuthor().getId());

            return new PostListResponse(
                    post.getId(),
                    post.getTitle(),
                    nickname,
                    totalViewCount,
                    post.getCreatedAt()
            );
        });
    }











}