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

    public PhantomTokenFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            String redisKey = redisTokenPrefix + token;
            String userIdString = redisTemplate.opsForValue().get(redisKey);

            if (StringUtils.hasText(userIdString)) {
                // 성공: 인증 정보 설정
                Long userId = Long.valueOf(userIdString);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // 실패: 토큰은 보냈지만 Redis에 없음 (만료 등)
                // 여기서 즉시 응답을 보내고 return 하여 filterChain 진행을 막습니다.
                sendErrorResponse(response, "유효하지 않거나 만료된 인증 토큰입니다.");
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