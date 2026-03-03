package lch.domain.user.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lch.global.security.HttpCookieOAuth2AuthorizationRequestRepository;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    public OAuth2FailureHandler(HttpCookieOAuth2AuthorizationRequestRepository cookieRepository) {
        this.cookieRepository = cookieRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // 1. 인증 과정에서 생성된 임시 쿠키들을 깔끔하게 삭제 (메모리 누수 및 오작동 방지)
        cookieRepository.removeAuthorizationRequestCookies(request, response);

        // 2. 에러 : CustomOAuth2UserService에서 던진 에러 메시지 추출
        String errorMessage = exception.getMessage();

        // 3. URL에 파라미터로 담기 위해 한글 메시지를 UTF-8로 인코딩
        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);

        // 4. 프론트엔드 처리용 URL 생성
        String targetUrl = redirectUri + "?error=" + encodedMessage;

        // 5. 프론트엔드로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}