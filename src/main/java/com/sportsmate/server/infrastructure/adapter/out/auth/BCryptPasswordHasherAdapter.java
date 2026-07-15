package com.sportsmate.server.infrastructure.adapter.out.auth;

import com.sportsmate.server.domain.member.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasherAdapter implements PasswordHasher {
    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasherAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    @Override public String hash(String rawPassword) { return passwordEncoder.encode(rawPassword); }
    @Override public boolean matches(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
