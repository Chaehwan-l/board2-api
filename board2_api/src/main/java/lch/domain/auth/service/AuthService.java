package lch.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lch.domain.auth.dto.RegisterCommand;
import lch.domain.user.entity.User;
import lch.domain.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 생성자 주입
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}