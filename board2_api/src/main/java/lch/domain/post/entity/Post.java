package lch.domain.post.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lch.domain.user.entity.User;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Long viewCount = 0L;

    private LocalDateTime createdAt = LocalDateTime.now();

    // 수정 시간 관리를 위한 필드
    private LocalDateTime updatedAt = LocalDateTime.now();

     // cascade = CascadeType.ALL: Post에 수행되는 모든 상태 변화(저장, 삭제 등)를 Attachment에 전달
     // orphanRemoval = true: Post의 attachments 리스트에서 요소가 제거되면, DB에서도 해당 Attachment를 삭제
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    // 댓글(Comment)도 마찬가지로 게시글 삭제 시 함께 삭제되어야 합니다.
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    protected Post() {} // JPA용 기본 생성자

    public Post(User author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;
    }

    // 게시글 수정 비즈니스 메서드 : 이 메서드가 호출되어 값이 바뀌면, Transaction 종료 시점에 자동으로 DB에 반영됨
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now(); // 수정 시 시간 갱신
    }

    // Redis의 누적 조회수를 DB에 더할 때 사용하는 비즈니스 메서드
    public void addViewCount(Long count) {
        this.viewCount += count;
    }

    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Long getViewCount() { return viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

}