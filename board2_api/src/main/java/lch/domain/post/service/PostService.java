package lch.domain.post.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import lch.domain.post.dto.AttachmentResponse;
import lch.domain.post.dto.CommentResponse;
import lch.domain.post.dto.PostCreateCommand;
import lch.domain.post.dto.PostListResponse;
import lch.domain.post.dto.PostResponse;
import lch.domain.post.dto.PostUpdateCommand;
import lch.domain.post.entity.Attachment;
import lch.domain.post.entity.Comment;
import lch.domain.post.entity.Post;
import lch.domain.post.repository.AttachmentRepository;
import lch.domain.post.repository.CommentRepository;
import lch.domain.post.repository.PostRepository;
import lch.domain.user.entity.User;
import lch.domain.user.repository.UserRepository;
import lch.domain.user.service.UserCacheService;
import lch.global.error.BusinessException;
import lch.global.infra.S3StorageService;

@Service
public class PostService {

	private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    private final RedisViewCountService viewCountService;
    private final UserCacheService userCacheService;
    private final S3StorageService s3StorageService;
    private final SearchService searchService;

    public PostService(PostRepository postRepository, AttachmentRepository attachmentRepository,
                       UserRepository userRepository, RedisViewCountService viewCountService,
                       SearchService searchService, UserCacheService userCacheService,
                       S3StorageService s3StorageService, CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.viewCountService = viewCountService;
        this.userCacheService = userCacheService;
        this.s3StorageService = s3StorageService;
        this.commentRepository = commentRepository;
        this.searchService = searchService;
    }

    @Transactional
    public Long createPost(PostCreateCommand command) {
        User author = userRepository.findById(command.authorId())
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        Post post = new Post(author, command.title(), command.content());
        postRepository.save(post);

        List<MultipartFile> files = command.files();
        if (files != null && !files.isEmpty()) {
            List<String> uploadedKeys = new ArrayList<>();

            for (MultipartFile file : files) {
                // S3 업로드
                String s3Key = s3StorageService.uploadFile(file);
                uploadedKeys.add(s3Key);

                // 트랜잭션 롤백 시 S3 파일 삭제를 위한 동기화 작업 등록
                registerS3Rollback(s3Key);

                Attachment attachment = new Attachment(post, s3Key, file.getOriginalFilename(), file.getSize());
                attachmentRepository.save(attachment);
            }
        }
        return post.getId();
    }

    // DB 트랜잭션 롤백 시 S3 파일을 삭제하는 동기화 로직
    private void registerS3Rollback(String s3Key) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        log.warn("DB 트랜잭션 롤백으로 인해 업로드된 S3 파일을 삭제합니다. key: {}", s3Key);
                        s3StorageService.deleteFile(s3Key);
                    }
                }
            });
        }
    }

    // 상세 조회
    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        // 조회수 증가 후, DB 값과 Redis에만 있는 미동기화 값을 합산하여 응답 (논리 오류 1-1 해결)
        viewCountService.increment(postId);
        Long redisCount = viewCountService.getCount("post:view:count:" + postId);
        Long totalViewCount = post.getViewCount() + redisCount;

        String authorNickname = userCacheService.getUserNickname(post.getAuthor().getId());

        List<AttachmentResponse> attachmentResponses = attachmentRepository.findByPostId(postId).stream()
                .map(a -> new AttachmentResponse(a.getId(), a.getFileName(), a.getS3Key())).toList();

        List<CommentResponse> commentResponses = commentRepository.findByPostId(postId).stream()
                .map(c -> new CommentResponse(c.getId(), c.getAuthor().getNickname(), c.getContent(), c.getCreatedAt()))
                .toList();

        return new PostResponse(
            post.getId(), post.getTitle(), post.getContent(), totalViewCount, // 합산값 전달
            authorNickname, post.getCreatedAt(), attachmentResponses, commentResponses
        );
    }

    // 삭제
    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new BusinessException.AccessDeniedException("게시글 삭제 권한이 없습니다.");
        }

        // 1. Redis 조회수 키 즉시 삭제 (메모리 누수 방지 - 논리 오류 1-3 해결)
        viewCountService.delete("post:view:count:" + postId);

        // 2. 연관된 첨부파일들을 찾아서 DB 커밋 성공 후에만 S3에서 실물 삭제 (정합성 보장 - 논리 오류 2-2 해결)
        List<Attachment> attachments = attachmentRepository.findByPostId(postId);
        for (Attachment attachment : attachments) {
            registerS3DeleteAfterCommit(attachment.getS3Key());
        }

        // 3. 게시글 삭제 (DB에서는 ON DELETE CASCADE에 의해 comments, attachments 행이 자동 삭제됨)
        postRepository.delete(post);
    }

    // DB 커밋 완료 후에만 S3 파일을 삭제하는 헬퍼 메서드
    private void registerS3DeleteAfterCommit(String s3Key) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("DB 삭제 완료. S3 파일을 실물 삭제합니다: {}", s3Key);
                    s3StorageService.deleteFile(s3Key);
                }
            });
        }
    }

    // 게시글 수정
    // updatePost 메서드 등 다른 로직에서도 동일하게 registerS3Rollback을 호출하여 안전하게 관리 가능합니다.
    @Transactional
    public Long updatePost(Long postId, Long currentUserId, PostUpdateCommand command) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new BusinessException.AccessDeniedException("게시글 수정 권한이 없습니다.");
        }

        post.update(command.title(), command.content());

        // 기존 파일 삭제
        if (command.deletedAttachmentIds() != null && !command.deletedAttachmentIds().isEmpty()) {
            List<Attachment> targetAttachments = attachmentRepository.findAllById(command.deletedAttachmentIds());
            for (Attachment attachment : targetAttachments) {
                if (attachment.getPost().getId().equals(postId)) {
                    s3StorageService.deleteFile(attachment.getS3Key());
                    attachmentRepository.delete(attachment);
                }
            }
        }

        // 새 파일 추가 시에도 롤백 로직 적용
        if (command.newFiles() != null && !command.newFiles().isEmpty()) {
            for (MultipartFile file : command.newFiles()) {
                String s3Key = s3StorageService.uploadFile(file);
                registerS3Rollback(s3Key); // 롤백 대비
                attachmentRepository.save(new Attachment(post, s3Key, file.getOriginalFilename(), file.getSize()));
            }
        }

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

    // 댓글 작성
    @Transactional
    public Long createComment(Long postId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        Comment comment = new Comment(post, author, content);
        return commentRepository.save(comment).getId();
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException.AccessDeniedException("댓글 삭제 권한이 없습니다.");
        }
        commentRepository.delete(comment);
    }

    // 게시글 검색 및 검색어 저장
    @Transactional(readOnly = true)
    public Page<PostListResponse> searchPosts(String keyword, Long userId, Pageable pageable) {
        // 검색어가 2자 이상일 때만 Redis에 저장 (무의미한 1글자 검색 방지)
        if (keyword != null && keyword.trim().length() >= 2) {
            searchService.saveKeyword(userId, keyword.trim());
        }

        Page<Post> posts = postRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);

        return posts.map(post -> {
            Long redisCount = viewCountService.getCount("post:view:count:" + post.getId());
            Long totalViewCount = post.getViewCount() + redisCount;
            String nickname = userCacheService.getUserNickname(post.getAuthor().getId());

            return new PostListResponse(
                    post.getId(), post.getTitle(), nickname, totalViewCount, post.getCreatedAt()
            );
        });
    }
}