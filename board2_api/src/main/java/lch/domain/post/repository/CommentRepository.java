package lch.domain.post.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import lch.domain.post.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // N+1 방지: 댓글 조회 시 작성자 정보도 함께 가져옴
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByPostId(Long postId);
}