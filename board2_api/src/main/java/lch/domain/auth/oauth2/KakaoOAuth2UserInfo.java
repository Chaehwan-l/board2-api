package lch.domain.auth.oauth2;

import java.util.Map;

// Record를 활용하여 불변 객체로 래핑합니다.
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
        // 카카오는 kakao_account 내부에 이메일이 있습니다.
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    public String getNickname() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        return properties != null ? (String) properties.get("nickname") : "카카오유저";
    }
}