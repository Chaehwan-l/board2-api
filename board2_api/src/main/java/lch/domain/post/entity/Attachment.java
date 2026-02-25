package lch.domain.post.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// 파일 첨부 엔티티

@Entity
@Table(name = "attachments")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected Attachment() {}

    public Attachment(Post post, String s3Key, String fileName, Long fileSize) {
        this.post = post;
        this.s3Key = s3Key;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    // Getters
    public Long getId() { return id; }
    public String getS3Key() { return s3Key; }
    public String getFileName() { return fileName; }
    public Long getFileSize() { return fileSize; }
    public Post getPost() { return post; }
}