package lch.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lch.domain.auth.oauth2.OAuth2SuccessHandler;
import lch.domain.auth.service.CustomOAuth2UserService;
import lch.global.security.PhantomTokenFilter;

// REST API이므로 불필요한 Form 로그인이나 CSRF 등은 비활성화하고, 세션 정책을 STATELESS로 설정

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PhantomTokenFilter phantomTokenFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // 커스텀 필터 주입
    public SecurityConfig(PhantomTokenFilter phantomTokenFilter,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.phantomTokenFilter = phantomTokenFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 단방향 해시 알고리즘인 BCrypt를 사용합니다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // REST API이므로 CSRF, HTTP Basic, Form Login 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)

            // JWT/팬텀 토큰을 사용하므로 Spring Security 기본 세션은 사용하지 않음
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 엔드포인트 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll() // 인증 없이 접근 가능
                .anyRequest().authenticated() // 나머지는 모두 인증 필요
            )

            // OAuth
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService) // 회원정보 저장
                    )
                    .successHandler(oAuth2SuccessHandler) // 성공 시 팬텀 토큰 발급 및 리다이렉트
                )


            // 기본 인증 필터 앞에 우리가 만든 팬텀 토큰 필터를 삽입
            .addFilterBefore(phantomTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}