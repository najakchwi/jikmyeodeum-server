package com.sportsmate.server.infrastructure.adapter.out.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryTokenStoreAdapter 단위 테스트")
class InMemoryTokenStoreAdapterTest {

    @Test
    @DisplayName("만료되지 않은 refreshToken으로 회원 ID를 조회할 수 있다")
    void findMemberIdByRefreshToken_beforeExpiry_returnsMemberId() {
        var tokenStore = new InMemoryTokenStoreAdapter();

        tokenStore.save("1", "refresh-token", 60);

        assertThat(tokenStore.findMemberIdByRefreshToken("refresh-token")).contains("1");
    }

    @Test
    @DisplayName("만료된 refreshToken은 조회 시 제거되고 empty를 반환한다")
    void findMemberIdByRefreshToken_afterExpiry_returnsEmpty() throws InterruptedException {
        var tokenStore = new InMemoryTokenStoreAdapter();

        tokenStore.save("1", "refresh-token", 0);
        Thread.sleep(5);

        assertThat(tokenStore.findMemberIdByRefreshToken("refresh-token")).isEmpty();
    }
}
