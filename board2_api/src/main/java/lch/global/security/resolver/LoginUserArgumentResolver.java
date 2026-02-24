package lch.global.security.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import lch.global.error.BusinessException;
import lch.global.security.annotation.LoginUser;

/*
 * Spring Web MVC가 컨트롤러의 파라미터를 바인딩할 때 :
 * 정의해 둔 @LoginUser가 붙어있으면 SecurityContext에서 유저 PK를 꺼내서 넣어주도록 처리하는 컴포넌트
 */

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 1. 파라미터에 @LoginUser 애노테이션이 붙어 있는지 확인
        boolean hasLoginUserAnnotation = parameter.hasParameterAnnotation(LoginUser.class);
        // 2. 파라미터의 타입이 Long (유저 PK)인지 확인
        boolean isLongType = Long.class.isAssignableFrom(parameter.getParameterType());

        return hasLoginUserAnnotation && isLongType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나, 익명 사용자일 경우
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // IllegalStateException(500 에러 원인) 대신 401을 유도하는 비즈니스 예외를 던짐
            throw new BusinessException.AuthenticationFailedException("인증 정보가 없거나 만료되었습니다.");
        }

        // 필터에서 주입했던 유저의 PK (Long)를 반환
        return authentication.getPrincipal();
    }
}