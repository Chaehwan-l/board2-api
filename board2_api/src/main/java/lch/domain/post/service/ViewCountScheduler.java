package lch.domain.post.service;

import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lch.domain.post.repository.PostRepository;

@Component
public class ViewCountScheduler {
    private final RedisViewCountService redisService;
    private final PostRepository postRepository;

    public ViewCountScheduler(RedisViewCountService redisService, PostRepository postRepository) {
        this.redisService = redisService;
        this.postRepository = postRepository;
    }

    @Scheduled(cron = "0 0/5 * * * *")
    @Transactional
    public void syncToDb() {
        Set<String> keys = redisService.getKeys(); // 이름 일치 확인
        if (keys == null || keys.isEmpty()) {
			return;
		}

        for (String key : keys) {
            Long postId = Long.parseLong(key.split(":")[3]);
            Long count = redisService.getCount(key);

            postRepository.findById(postId).ifPresent(post -> {
                post.addViewCount(count);
                redisService.delete(key); // DB 반영 성공 시 Redis 삭제
            });
        }
    }
}