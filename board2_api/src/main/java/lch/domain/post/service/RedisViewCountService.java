package lch.domain.post.service;

import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/*
 * Write-back 전략
 * 조회수가 발생할 때마다 DB에 바로 반영하는 것이 아니라,
 * Redis의 원자적 연산(INCR)으로 빠르게 카운팅한 뒤 스케줄러를 통해 주기적으로 DB에 일괄 반영
 */

// Redis에서 조회수를 관리하는 서비스

@Service
public class RedisViewCountService {
    private final StringRedisTemplate redisTemplate;
    private static final String VIEW_COUNT_PREFIX = "post:view:count:";

    public RedisViewCountService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Redis에서 해당 게시글의 조회수 증가 (Atomic 연산)
    public void increment(Long postId) {
        redisTemplate.opsForValue().increment(VIEW_COUNT_PREFIX + postId);
    }

    // 스케줄러가 읽어갈 모든 조회수 키 목록 조회
    public Set<String> getKeys() {
        return redisTemplate.keys(VIEW_COUNT_PREFIX + "*");
    }

    // 특정 키의 조회수 값을 가져옴
    public Long getCount(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0L;
    }

    // DB 반영 완료 후 Redis 데이터 삭제
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}