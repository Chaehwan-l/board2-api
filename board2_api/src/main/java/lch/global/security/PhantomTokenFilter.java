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

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 변환을 위해 추가

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lch.global.common.ApiResponse; // 공통 응답 규격 사용

@Component
public class PhantomTokenFilter extends OncePerRequestFilter {

    @Value("${app.token.redis-prefix}")
    private String redisTokenPrefix;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 처리를 위한 객체
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
                    // JWT가 변조되었거나 만료된 경우
                    sendErrorResponse(response, "인증 정보가 유효하지 않습니다.");
                    return;
                }
            } else {
                // UUID 자체가 Redis에 없는 경우
                sendErrorResponse(response, "로그인 세션이 만료되었습니다.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // JSON 형태의 에러 응답을 직접 만드는 헬퍼 메서드
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
        response.setContentType("application/json;charset=UTF-8");

        // 프로젝트 공통 규격인 ApiResponse.error() 활용
        ApiResponse<Void> apiResponse = ApiResponse.error(message);
        String json = objectMapper.writeValueAsString(apiResponse);

        response.getWriter().write(json);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}