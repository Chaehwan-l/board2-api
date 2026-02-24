package lch.domain.post.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lch.domain.post.entity.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    // 게시글에 속한 모든 첨부파일을 찾기 위한 메서드
    List<Attachment> findByPostId(Long postId);
}