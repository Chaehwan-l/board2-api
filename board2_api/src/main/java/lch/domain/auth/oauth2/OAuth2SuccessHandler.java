package lch.domain.auth.oauth2;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lch.domain.auth.service.CustomOAuth2User;

// Redis 토큰 저장 및 프론트로의 리다이렉트 처리

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    private static final String REDIS_TOKEN_PREFIX = "auth:token:";
    private static final long TOKEN_EXPIRATION_HOURS = 2;

    public OAuth2SuccessHandler(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        // CustomOAuth2User에서 유저 PK를 꺼냄
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Long userId = oAuth2User.getId();

        // 팬텀 토큰 발급
        String phantomToken = UUID.randomUUID().toString();
        String redisKey = REDIS_TOKEN_PREFIX + phantomToken;

        // Redis에 토큰과 유저 ID 매핑 저장
        redisTemplate.opsForValue().set(
                redisKey,
                String.valueOf(userId),
                Duration.ofHours(TOKEN_EXPIRATION_HOURS)
        );

        // 프론트엔드 URL로 토큰을 파라미터에 담아 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", phantomToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}