package lch.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import lch.domain.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
}