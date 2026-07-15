package com.sportsmate.server.domain.member.policy;

import java.util.regex.Pattern;

public final class PasswordPolicy {
    public static final String INVALID_PASSWORD_MESSAGE = "비밀번호는 영문, 숫자를 포함해 8자 이상이어야 해요";

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isBlank(String password) {
        return password == null || password.isBlank();
    }
}
