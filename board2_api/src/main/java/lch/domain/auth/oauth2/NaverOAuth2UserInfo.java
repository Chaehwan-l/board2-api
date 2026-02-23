package lch.domain.auth.oauth2;

import java.util.Map;

public record NaverOAuth2UserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    // 네이버는 response 객체 안에 정보가 담겨 옵니다.
    private Map<String, Object> getResponse() {
        return (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderId() {
        return (String) getResponse().get("id");
    }

    @Override
    public String getProvider() {
        return "NAVER";
    }

    @Override
    public String getEmail() {
        return (String) getResponse().get("email");
    }

    @Override
    public String getNickname() {
        return (String) getResponse().get("nickname");
    }
}