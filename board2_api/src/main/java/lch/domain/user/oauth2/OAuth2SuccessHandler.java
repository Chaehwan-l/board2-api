package lch.domain.user.oauth2;

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
import lch.domain.user.service.CustomOAuth2User;
import lch.global.security.HttpCookieOAuth2AuthorizationRequestRepository;
import lch.global.security.JwtProvider;

// Redis 토큰 저장 및 내부 JWT 처리

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final StringRedisTemplate redisTemplate;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Value("${app.token.expiration-hours}")
    private long tokenExpirationHours;

    @Value("${app.token.redis-prefix}")
    private String redisTokenPrefix;

    public OAuth2SuccessHandler(StringRedisTemplate redisTemplate,
    							HttpCookieOAuth2AuthorizationRequestRepository cookieRepository,
    							JwtProvider jwtProvider) {
        this.redisTemplate = redisTemplate;
        this.cookieRepository = cookieRepository;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        // CustomOAuth2User에서 유저 PK를 꺼냄
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // 1. JWT 발급
        String internalJwt = jwtProvider.createToken(oAuth2User.getId(), "ROLE_USER");
        String phantomToken = UUID.randomUUID().toString();

        // 2. Redis 저장
        redisTemplate.opsForValue().set(
                redisTokenPrefix + phantomToken,
                internalJwt,
                Duration.ofHours(tokenExpirationHours)
        );

        // 프론트엔드로 넘어가기 전, 인증 과정에서 생성했던 임시 쿠키를 메모리에서 깔끔하게 삭제
        cookieRepository.removeAuthorizationRequestCookies(request, response);

        // 프론트엔드 URL로 토큰을 파라미터에 담아 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", phantomToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);

    }
}