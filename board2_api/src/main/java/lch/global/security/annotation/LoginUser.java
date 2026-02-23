package lch.global.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/*
 * 유저 식별자를 꺼내 쓰기 위해 매번 SecurityContextHolder.getContext().getAuthentication().getPrincipal()을 호출 시 코드가 너무 복잡해짐
 * 대신 커스텀 애노테이션(@LoginUser)과 ArgumentResolver를 활용하여, 컨트롤러의 파라미터로 로그인한 유저의 ID를 깔끔하게 주입받는 방식을 사용
 */

// 컨트롤러 메서드의 파라미터에만 붙일 수 있도록 설정
@Target(ElementType.PARAMETER)
// 런타임 시점까지 애노테이션 정보가 유지되도록 설정
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}