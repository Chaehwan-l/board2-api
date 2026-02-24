package lch.domain.user.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lch.domain.user.entity.User;

// Spring Security가 다루는 OAuth2 인증 객체 규격을 맞추기 위해 인증된 유저의 정보와 PK를 담아두는 객체

public class CustomOAuth2User implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    // 생성자 주입
    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    // ★ 가장 중요한 메서드: 이 메서드를 통해 SuccessHandler에서 유저의 PK를 꺼낼 수 있습니다.
    public Long getId() {
        return user.getId();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 유저의 권한(Role)을 Security가 인식할 수 있는 형태로 변환하여 반환
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public String getName() {
        // 식별자 반환 (보통 이메일이나 PK를 반환합니다)
        return user.getEmail();
    }
}