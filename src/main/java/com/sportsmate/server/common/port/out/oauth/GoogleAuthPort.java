package com.sportsmate.server.common.port.out.oauth;

/**
 * 구글 ID Token을 검증하고 사용자 정보를 조회하는 포트.
 */
public interface GoogleAuthPort {

    SocialUserInfo verify(String idToken);
}
