package lch.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Swagger UI 설정

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "bearerAuth";

        // 1. 보안 요구사항 정의 (전역적으로 해당 인증 방식을 사용하도록 설정)
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        // 2. 보안 스키마 정의 (Authorization: Bearer <token> 형태임을 명시)
        SecurityScheme securityScheme = new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("UUID"); // 우리 프로젝트는 UUID 기반 팬텀 토큰을 사용하므로 힌트 제공

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes(securitySchemeName, securityScheme));
    }
}