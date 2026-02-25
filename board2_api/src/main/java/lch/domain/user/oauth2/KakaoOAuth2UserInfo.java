package lch.domain.user.oauth2;

import java.util.Map;

public record KakaoOAuth2UserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {
    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "KAKAO";
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        // 카카오에서 이메일을 제공하지 않으므로, 고유한 더미 이메일 생성
        // providerId는 카카오 유저마다 고유한 숫자값이므로 UNIQUE 제약조건을 안전하게 통과함
        if (email == null || email.isBlank()) {
            return "kakao_" + getProviderId() + "@dummy.com";
        }

        return email;
    }

    @Override
    public String getNickname() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        return properties != null ? (String) properties.get("nickname") : "카카오유저";
    }
}