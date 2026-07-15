package com.sportsmate.server.common.vo;

import java.util.regex.Pattern;

/**
 * PhoneNumber 값 객체 - 국내 휴대전화번호 형식 검증과 값 동등성을 담당한다.
 * "-" 구분자가 포함된 입력은 정규화하여 숫자만 저장한다.
 */
public record PhoneNumber(String value) {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[0-9]\\d{7,8}$");

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be blank");
        }

        value = value.replace("-", "").trim();

        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
