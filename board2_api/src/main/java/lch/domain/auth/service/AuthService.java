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


@Service
public class AuthService {

	private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.token.expiration-hours}")
    private long tokenExpirationHours;

    @Value("${app.token.redis-prefix}")
    private String redisTokenPrefix;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Long registerLocalUser(RegisterCommand command) {

        // 1. 중복 검증 (예외가 발생하면 GlobalExceptionHandler가 낚아채서 409 응답을 만듦)
        if (userRepository.existsByUserId(command.userId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(command.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
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
        // 1. 유저 조회
        User user = userRepository.findByUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // 2. 비밀번호 검증 (입력된 평문 비밀번호와 DB의 해시된 비밀번호 비교)
        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."); // 보안상 동일한 메시지 반환
        }

        // 3. 팬텀 토큰 생성 (클라이언트에게 노출될 랜덤 문자열)
        String phantomToken = UUID.randomUUID().toString();

        // 4. Redis에 토큰 저장 (Key: auth:token:랜덤문자열, Value: 유저 PK ID)
        // 실무에서는 userId나 role 정보를 JSON 형태로 묶어서 Value로 저장하기도 합니다.
        String redisKey = redisTokenPrefix + phantomToken;
        redisTemplate.opsForValue().set(
                redisKey,
                String.valueOf(user.getId()),
                Duration.ofHours(tokenExpirationHours)
        );

        return phantomToken; // 컨트롤러로 토큰 반환
    }

}