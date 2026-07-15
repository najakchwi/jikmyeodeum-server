package com.sportsmate.server.common.vo;

import java.util.regex.Pattern;

/**
 * Email 값 객체 - 서비스 전역에서 이메일 형식 검증과 값 동등성을 담당한다.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException("Email cannot contain leading or trailing whitespace");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
