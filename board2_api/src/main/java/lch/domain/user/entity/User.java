package lch.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, length = 50)
    private String userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 20)
    private String provider; // LOCAL, NAVER, KAKAO

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false, length = 20)
    private String role; // ROLE_USER

    // JPA용 기본 생성자
    protected User() {}

    // 팩토리메서드에서만 사용 가능한 private 생성자
    private User(String userId, String email, String password, String nickname, String provider, String providerId, String role) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    // 로컬 가입 전용 팩토리 메서드
    public static User createLocalUser(String userId, String email, String encodedPassword, String nickname) {
        return new User(userId, email, encodedPassword, nickname, "LOCAL", null, "ROLE_USER");
    }

    public static User createOAuthUser(String email, String nickname, String provider, String providerId) {
        return new User(null, email, null, nickname, provider, providerId, "ROLE_USER");
    }

    // Getter 메서드들
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    public String getProvider() { return provider; }
    public String getRole() { return role; }
}