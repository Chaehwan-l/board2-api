package lch.domain.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lch.domain.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	// N+1 문제 방지: 게시글 조회 시 작성자(author) 정보도 함께 JOIN 페치
	@Override
	@EntityGraph(attributePaths = {"author"})
    Page<Post> findAll(Pageable pageable);

	// 제목 또는 내용으로 검색 (N+1 방지)
    @EntityGraph(attributePaths = {"author"})
    Page<Post> findByTitleContainingOrContentContaining(String title, String content, Pageable pageable);

    // 조회수 합산 시 영속성 컨텍스트의 1차 캐시 문제로 데이터가 덮어씌워지는 것을 방지하기 위해 DB 수준에서 직접 더하는 쿼리를 사용
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + :count WHERE p.id = :postId")
    void addViewCountNative(@Param("postId") Long postId, @Param("count") Long count);
}