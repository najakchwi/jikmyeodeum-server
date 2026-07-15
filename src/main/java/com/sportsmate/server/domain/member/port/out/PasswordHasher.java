package com.sportsmate.server.domain.member.port.out;

public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hashedPassword);
}
