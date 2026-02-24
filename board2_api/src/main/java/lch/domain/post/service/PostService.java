package lch.domain.post.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lch.domain.post.dto.AttachmentResponse;
import lch.domain.post.dto.PostCreateCommand;
import lch.domain.post.dto.PostListResponse;
import lch.domain.post.dto.PostResponse;
import lch.domain.post.dto.PostUpdateCommand;
import lch.domain.post.entity.Attachment;
import lch.domain.post.entity.Post;
import lch.domain.post.repository.AttachmentRepository;
import lch.domain.post.repository.PostRepository;
import lch.domain.user.entity.User;
import lch.domain.user.repository.UserRepository;
import lch.domain.user.service.UserCacheService;
import lch.global.error.BusinessException;
import lch.global.infra.S3StorageService;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final RedisViewCountService viewCountService;
    private final UserCacheService userCacheService;
    private final S3StorageService s3StorageService;

    public PostService(PostRepository postRepository,
                       AttachmentRepository attachmentRepository,
                       UserRepository userRepository,
                       RedisViewCountService viewCountService,
                       SearchService searchService,
                       UserCacheService userCacheService,
                       S3StorageService s3StorageService) {
        this.postRepository = postRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.viewCountService = viewCountService;
        this.userCacheService = userCacheService;
        this.s3StorageService = s3StorageService;
    }

    @Transactional
    public Long createPost(PostCreateCommand command) {
        User author = userRepository.findById(command.authorId())
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        // 1. 게시글 저장
        Post post = new Post(author, command.title(), command.content());
        postRepository.save(post);

        // 2. 첨부파일이 존재할 경우 S3 업로드 및 DB 저장
        List<MultipartFile> files = command.files();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                // S3에 파일 업로드 후 경로(Key) 반환
                String s3Key = s3StorageService.uploadFile(file);

                // DB에 메타데이터 저장
                Attachment attachment = new Attachment(post, s3Key, file.getOriginalFilename(), file.getSize());
                attachmentRepository.save(attachment);
            }
        }
        return post.getId();
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        viewCountService.increment(postId);
        String authorNickname = userCacheService.getUserNickname(post.getAuthor().getId());

        // 해당 게시글의 첨부파일 목록 조회
        List<AttachmentResponse> attachmentResponses = attachmentRepository.findByPostId(postId).stream()
                .map(a -> new AttachmentResponse(a.getId(), a.getFileName(), a.getS3Key()))
                .toList();

        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getViewCount(),
            authorNickname,
            post.getCreatedAt(),
            attachmentResponses
        );
    }

    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new BusinessException.AccessDeniedException("게시글 삭제 권한이 없습니다.");
        }

        // 1. 연관된 첨부파일들을 찾아서 S3에서 실물 삭제
        List<Attachment> attachments = attachmentRepository.findByPostId(postId);
        for (Attachment attachment : attachments) {
            s3StorageService.deleteFile(attachment.getS3Key());
        }

        // 2. 게시글 삭제 (DB에서는 ON DELETE CASCADE에 의해 comments, attachments 행이 자동 삭제됨)
        postRepository.delete(post);
    }

    @Transactional
    public Long updatePost(Long postId, Long currentUserId, PostUpdateCommand command) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new BusinessException.AccessDeniedException("게시글 수정 권한이 없습니다.");
        }

        post.update(command.title(), command.content());
        return post.getId();
    }

    @Transactional(readOnly = true)
    public Page<PostListResponse> getPostList(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);

        return posts.map(post -> {
            Long redisCount = viewCountService.getCount("post:view:count:" + post.getId());
            Long totalViewCount = post.getViewCount() + redisCount;
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