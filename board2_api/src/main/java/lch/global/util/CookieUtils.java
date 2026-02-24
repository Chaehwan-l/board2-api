package lch.global.util;

import java.util.Base64;
import java.util.Optional;

import org.springframework.util.SerializationUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
 * API 서버로서의 정체성(STATELESS 세션 정책)을 유지하기 위하여
 * OAuth2 로그인 시 발생하는 상태(State) 검증 데이터를 세션이 아닌 쿠키에 임시 저
 */

// OAuth2 상태 정보를 쿠키에 직렬화/역직렬화하여 저장하기 위한 유틸리티 클래스

public class CookieUtils {

    // 특정 이름의 쿠키를 찾아 반환
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    // 쿠키 생성 및 응답 객체에 추가
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // XSS 공격 방지를 위해 HttpOnly 설정
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    // 쿠키 만료(삭제) 처리
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    // 객체(OAuth2 인증 요청 정보)를 String으로 직렬화
    public static String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    // String으로 저장된 쿠키 값을 원래의 객체 타입으로 역직렬화
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}