package lch.global.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lch.global.security.resolver.LoginUserArgumentResolver;

// 작성한 ArgumentResolver를 Spring MVC가 인식할 수 있도록 설정 파일에 등록

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginUserArgumentResolver loginUserArgumentResolver;

    // 생성자 주입
    public WebMvcConfig(LoginUserArgumentResolver loginUserArgumentResolver) {
        this.loginUserArgumentResolver = loginUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 커스텀 리졸버 추가
        resolvers.add(loginUserArgumentResolver);
    }
}