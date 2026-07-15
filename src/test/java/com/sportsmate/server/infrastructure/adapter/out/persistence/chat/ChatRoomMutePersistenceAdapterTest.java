package com.sportsmate.server.infrastructure.adapter.out.persistence.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sportsmate.server.infrastructure.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ChatRoomMutePersistenceAdapter.class, JpaConfig.class})
@DisplayName("ChatRoomMutePersistenceAdapter JPA 테스트")
class ChatRoomMutePersistenceAdapterTest {

    @Autowired
    ChatRoomMutePersistenceAdapter adapter;

    @Test
    @DisplayName("한 번도 mute한 적 없는 방을 unmute해도 예외가 나지 않는다")
    void unmute_neverMuted_doesNotThrow() {
        assertThatCode(() -> adapter.unmute(1L, "chat_1")).doesNotThrowAnyException();
        assertThat(adapter.isMuted(1L, "chat_1")).isFalse();
    }

    @Test
    @DisplayName("mute 후 unmute하면 상태가 해제된다")
    void mute_thenUnmute_clearsMute() {
        adapter.mute(1L, "chat_1");
        assertThat(adapter.isMuted(1L, "chat_1")).isTrue();

        adapter.unmute(1L, "chat_1");

        assertThat(adapter.isMuted(1L, "chat_1")).isFalse();
    }

    @Test
    @DisplayName("같은 방을 두 번 unmute해도 예외가 나지 않는다")
    void unmute_calledTwice_doesNotThrow() {
        adapter.mute(1L, "chat_1");
        adapter.unmute(1L, "chat_1");

        assertThatCode(() -> adapter.unmute(1L, "chat_1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("같은 방을 두 번 mute해도 예외가 나지 않는다")
    void mute_calledTwice_doesNotThrow() {
        adapter.mute(1L, "chat_1");

        assertThatCode(() -> adapter.mute(1L, "chat_1")).doesNotThrowAnyException();
        assertThat(adapter.isMuted(1L, "chat_1")).isTrue();
    }
}
