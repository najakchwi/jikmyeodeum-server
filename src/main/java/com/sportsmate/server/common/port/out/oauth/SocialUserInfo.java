package com.sportsmate.server.common.port.out.oauth;

/**
 * 소셜 로그인 제공자로부터 검증된 사용자 정보.
 */
public record SocialUserInfo(String providerId, String email, String name) {
}
