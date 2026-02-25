package lch.global.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PhantomTokenFilter extends OncePerRequestFilter {

    @Value("${app.token.redis-prefix}")
    private String redisTokenPrefix;

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;

    public PhantomTokenFilter(StringRedisTemplate redisTemplate, JwtProvider jwtProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            String jwt = redisTemplate.opsForValue().get(redisTokenPrefix + token);

            if (StringUtils.hasText(jwt)) {
                try {
                    // 1. Redis에서 꺼낸 JWT 파싱
                    Claims claims = jwtProvider.getClaims(jwt);
                    Long userId = Long.valueOf(claims.getSubject());
                    String role = claims.get("role", String.class);

                    // 2. SecurityContext에 인증 정보 주입
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null,
                                    Collections.singletonList(new SimpleGrantedAuthority(role)));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (Exception e) {
                	// 검증 실패 시 로그만 남기는 것이 아니라 Redis에 저장된 잘못된 토큰 정보를 즉시 삭제
                    redisTemplate.delete(redisTokenPrefix + token);
                    logger.error("Invalid or corrupted JWT token removed from Redis: " + e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. HTTP 헤더에서 토큰을 먼저 확인합니다. (일반 폼 로그인의 경우)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. 헤더에 토큰이 없다면, 브라우저가 보낸 쿠키에서 토큰을 확인합니다. (OAuth2 로그인의 경우)
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                // OAuth2SuccessHandler에서 구워준 쿠키 이름("access_token")과 일치하는지 확인
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 3. 둘 다 없으면 null 반환 (인증 실패 처리됨)
        return null;
    }
}