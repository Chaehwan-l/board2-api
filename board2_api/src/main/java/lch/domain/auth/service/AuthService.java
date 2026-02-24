package lch.domain.auth.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lch.domain.auth.dto.LoginCommand;
import lch.domain.auth.dto.RegisterCommand;
import lch.domain.user.entity.User;
import lch.domain.user.repository.UserRepository;
import lch.global.error.BusinessException;
import lch.global.security.JwtProvider;


@Service
public class AuthService {

	private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;

    @Value("${app.token.expiration-hours}")
    private long tokenExpirationHours;

    @Value("${app.token.redis-prefix}")
    private String redisTokenPrefix;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
    					StringRedisTemplate redisTemplate, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public Long registerLocalUser(RegisterCommand command) {

    	// 중복 검증 시 DuplicateResourceException 사용
    	if (userRepository.existsByUserId(command.userId())) {
            throw new BusinessException.DuplicateResourceException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new BusinessException.DuplicateResourceException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(command.nickname())) {
            throw new BusinessException.DuplicateResourceException("이미 사용 중인 닉네임입니다.");
        }

        // 2. 패스워드 암호화
        String encodedPassword = passwordEncoder.encode(command.password());

        // 3. 팩토리 메서드를 통한 안전한 엔티티 생성
        User newUser = User.createLocalUser(
                command.userId(),
                command.email(),
                encodedPassword,
                command.nickname()
        );

        userRepository.save(newUser);

        return newUser.getId();
    }

    @Transactional(readOnly = true)
    public String login(LoginCommand command) {

    	// 로그인 실패 시 AuthenticationFailedException 사용
    	User user = userRepository.findByUserId(command.userId())
                .orElseThrow(() -> new BusinessException.AuthenticationFailedException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessException.AuthenticationFailedException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 1. 내부용 JWT 발급
        String internalJwt = jwtProvider.createToken(user.getId(), user.getRole());

        // 2. 외부용 UUID 발급
        String phantomToken = UUID.randomUUID().toString();

        // 3. Redis 저장 [UUID : JWT]
        redisTemplate.opsForValue().set(
                redisTokenPrefix + phantomToken,
                internalJwt,
                Duration.ofHours(tokenExpirationHours)
        );

        return phantomToken;
    }

}