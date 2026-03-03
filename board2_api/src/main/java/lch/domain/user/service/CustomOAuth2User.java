package lch.domain.user.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lch.domain.user.entity.User;

public class CustomOAuth2User implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public String getName() {
        // 기존 return user.getEmail(); 에서 변경 :
        // Spring Security가 인증 객체를 식별하는 고유 키로 DB의 PK를 사용하도록 함
        // 반환 타입이 String이므로 String.valueOf()를 통해 형변환하여 반환
        return String.valueOf(user.getId());
    }
}