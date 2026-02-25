package lch.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lch.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);
    Optional<User> findByEmail(String email);

    // OAuth : provieder, provider_id로 유저를 엄격하게 식별 (보안 강화)
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // 중복 검증용
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}