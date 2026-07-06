package com.usang.marketdata.api.auth;

import com.usang.marketdata.domain.user.User;
import com.usang.marketdata.domain.user.UserRepository;
import com.usang.marketdata.infra.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@RequestBody AuthRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        String hash = passwordEncoder.encode(request.password());
        userRepository.save(User.of(request.username(), hash));
        return new TokenResponse(jwtTokenProvider.generateToken(request.username()));
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 틀렸습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 틀렸습니다.");
        }
        return new TokenResponse(jwtTokenProvider.generateToken(request.username()));
    }

    record AuthRequest(String username, String password) {}
    record TokenResponse(String token) {}
}
