package lch.domain.post.service;

import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate; // 추가
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lch.domain.post.repository.PostRepository;

@Component
public class ViewCountScheduler {
    private final RedisViewCountService redisService;
    private final PostRepository postRepository;
    private final StringRedisTemplate redisTemplate;

    public ViewCountScheduler(RedisViewCountService redisService, PostRepository postRepository, StringRedisTemplate redisTemplate) {
        this.redisService = redisService;
        this.postRepository = postRepository;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(cron = "0 0/5 * * * *")
    @Transactional
    public void syncToDb() {
        Set<String> keys = redisService.getKeys();
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            // 원자적으로 값을 가져오고 동시에 삭제하여 유실 방지 (논리 오류 1-2 해결)
            String val = redisTemplate.opsForValue().getAndDelete(key);
            if (val == null) {
                continue;
            }

            // 프리픽스 제거 로직 개선 (단순 인덱스 기반보다 안전)
            Long postId = Long.parseLong(key.replace("post:view:count:", ""));

            // Native Query 호출: 1차 캐시 문제 없이 DB 수준에서 즉시 합산
            postRepository.addViewCountNative(postId, Long.parseLong(val));
        }
    }
}