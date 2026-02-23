package lch.global.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
 * 프론트엔드는 로그인 후 발급받은 UUID를 Authorization: Bearer <UUID> 형태로 보냄
 * 백엔드는 JWT처럼 Secret Key로 복호화하는 대신, 빠르고 안전한 in-memory 캐시인 Redis를 찔러보고 유저를 식별
 */

// 클라이언트가 API 요청을 보낼 때 헤더에 포함된 토큰을 가로채어, Redis를 통해 검증하고 SecurityContext에 인증 객체를 심어주는 핵심 필터

@Component
public class PhantomTokenFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_TOKEN_PREFIX = "auth:token:";

    public PhantomTokenFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Request Header에서 토큰 추출 (Bearer ...)
        String token = resolveToken(request);

        // 2. 토큰이 존재하면 Redis 검증
        if (StringUtils.hasText(token)) {
            String redisKey = REDIS_TOKEN_PREFIX + token;
            String userIdString = redisTemplate.opsForValue().get(redisKey);

            if (StringUtils.hasText(userIdString)) {
                // 3. Redis에 토큰이 유효하다면 인증 객체 생성
                // (실제 실무에서는 Redis에 Role도 함께 저장하여 파싱하거나, 여기서 DB를 한 번 더 조회하기도 합니다.
                // 여기서는 성능을 위해 기본 권한 ROLE_USER를 우선 부여합니다.)
                Long userId = Long.valueOf(userIdString);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

                // 4. SecurityContext에 인증 정보 저장 (이후 컨트롤러에서 @AuthenticationPrincipal로 꺼내 쓸 수 있음)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 5. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    // 헤더에서 Bearer 토큰 파싱하는 유틸 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 이후의 실제 토큰 부분만 잘라서 반환
        }
        return null;
    }
}