package lch.domain.post.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final StringRedisTemplate redisTemplate;
    private static final String SEARCH_HISTORY_PREFIX = "search:history:";

    public SearchService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 최근 검색어 저장 (최근 10개 유지)
    public void saveKeyword(Long userId, String keyword) {
        String key = SEARCH_HISTORY_PREFIX + userId;
        double score = System.currentTimeMillis();

        // ZSET: 동일 키워드 입력 시 스코어(시간)만 업데이트되어 중복 방지 및 순서 유지
        redisTemplate.opsForZSet().add(key, keyword, score);

        // 10개 초과 시 오래된 순서(인덱스 0번부터)대로 삭제
        redisTemplate.opsForZSet().removeRange(key, 0, -11);
    }

    // 최근 검색어 목록 조회 (최신순)
    public List<String> getHistory(Long userId) {
        String key = SEARCH_HISTORY_PREFIX + userId;
        // 스코어 역순(최신순)으로 10개 조회
        Set<String> history = redisTemplate.opsForZSet().reverseRange(key, 0, 9);

        return history != null ? history.stream().toList() : List.of();
    }

    // 검색 기록 전체 삭제 (로그아웃 연동용)
    public void clearHistory(Long userId) {
        redisTemplate.delete(SEARCH_HISTORY_PREFIX + userId);
    }
}