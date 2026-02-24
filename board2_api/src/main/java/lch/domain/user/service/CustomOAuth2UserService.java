package lch.domain.user.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lch.domain.user.entity.User;
import lch.domain.user.oauth2.KakaoOAuth2UserInfo;
import lch.domain.user.oauth2.NaverOAuth2UserInfo;
import lch.domain.user.oauth2.OAuth2UserInfo;
import lch.domain.user.repository.UserRepository;

/*
 * CustomOAuth2User 클래스는 OAuth2User 인터페이스를 구현하며,
 * 내부적으로 유저 PK(Long ID)를 반환하도록 설계해야 나중에 SuccessHandler에서 PK를 뽑아 팬텀 토큰에 매핑 가능
 */

// Spring Security가 제공하는 DefaultOAuth2UserService를 상속받아, 받아온 유저 정보를 DB에 저장하거나 업데이트

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 카카오인지 네이버인지 식별
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo;
        if ("kakao".equals(registrationId)) {
            userInfo = new KakaoOAuth2UserInfo(oAuth2User.getAttributes());
        } else if ("naver".equals(registrationId)) {
            userInfo = new NaverOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        // DB에서 제공자와 제공자ID로 유저를 조회, 없으면 생성
        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> createUser(userInfo));

        // CustomOAuth2User는 OAuth2User 인터페이스를 구현한 내부/별도 클래스 (PK를 SecurityContext에 담기 위해 확장)
        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User createUser(OAuth2UserInfo userInfo) {
        // User 엔티티에 만들어둔 OAuth 유저용 정적 팩토리 메서드 활용
        User newUser = User.createOAuthUser(
                userInfo.getEmail(),
                userInfo.getNickname(),
                userInfo.getProvider(),
                userInfo.getProviderId()
        );
        return userRepository.save(newUser);
    }
}