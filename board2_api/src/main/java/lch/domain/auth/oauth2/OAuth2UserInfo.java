package lch.domain.auth.oauth2;

// 카카오와 네이버는 유저 정보를 반환하는 JSON 구조가 다름 : 하나의 인터페이스로 묶어 처리

public interface OAuth2UserInfo {
    String getProviderId(); // 각 제공자의 고유 식별자
    String getProvider();   // KAKAO, NAVER
    String getEmail();
    String getNickname();
}