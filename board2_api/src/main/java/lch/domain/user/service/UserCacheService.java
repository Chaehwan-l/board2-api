package lch.domain.user.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lch.domain.user.repository.UserRepository;

@Service
public class UserCacheService {
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    public UserCacheService(StringRedisTemplate redisTemplate, UserRepository userRepository) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
    }

    // Cache-aside: 유저 닉네임 조회 시 캐싱 적용
    public String getUserNickname(Long userId) {
        String cacheKey = "user:nickname:" + userId;
        String cachedNickname = redisTemplate.opsForValue().get(cacheKey);

        if (cachedNickname != null) {
			return cachedNickname;
		}

        String nickname = userRepository.findById(userId)
                .map(user -> user.getNickname())
                .orElse("알 수 없는 사용자");

        // 1시간 동안 캐싱
        redisTemplate.opsForValue().set(cacheKey, nickname, Duration.ofHours(1));
        return nickname;
    }
}