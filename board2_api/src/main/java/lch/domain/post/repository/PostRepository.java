package lch.domain.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import lch.domain.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	// N+1 문제 방지: 게시글 조회 시 작성자(author) 정보도 함께 JOIN 페치
	@Override
	@EntityGraph(attributePaths = {"author"})
    Page<Post> findAll(Pageable pageable);

	// 제목 또는 내용으로 검색 (N+1 방지)
    @EntityGraph(attributePaths = {"author"})
    Page<Post> findByTitleContainingOrContentContaining(String title, String content, Pageable pageable);

}