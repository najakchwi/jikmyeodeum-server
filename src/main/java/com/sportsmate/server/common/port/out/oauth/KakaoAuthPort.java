package com.sportsmate.server.common.port.out.oauth;

/**
 * 카카오 ID Token을 검증하고 사용자 정보를 조회하는 포트.
 */
public interface KakaoAuthPort {

    SocialUserInfo verify(String idToken);
}
