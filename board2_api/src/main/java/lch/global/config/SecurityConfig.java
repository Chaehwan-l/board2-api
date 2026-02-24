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

import jakarta.servlet.http.HttpServletResponse;
import lch.domain.auth.oauth2.OAuth2SuccessHandler;
import lch.domain.auth.service.CustomOAuth2UserService;
import lch.global.security.PhantomTokenFilter;
import lch.global.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PhantomTokenFilter phantomTokenFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    // 커스텀 필터 및 핸들러, 쿠키 저장소 주입
    public SecurityConfig(PhantomTokenFilter phantomTokenFilter,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository) {
        this.phantomTokenFilter = phantomTokenFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.cookieAuthorizationRequestRepository = cookieAuthorizationRequestRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS는 배제, REST API이므로 CSRF 및 기본 로그인 폼 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)

            // API 서버의 핵심: 세션을 사용하지 않음 (STATELESS)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register", "/auth/login", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )

            // 인증 실패 시 HTML 로그인 페이지 대신 JSON을 반환하도록 설정
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다.\",\"data\":null}");
                })
            )

            // OAuth2 설정
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    // 세션 대신 커스텀 쿠키 저장소를 사용하여 Stateless 환경에서 인증 상태를 유지함
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository)
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
            )

            // 우리가 만든 팬텀 토큰 필터 삽입
            .addFilterBefore(phantomTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}