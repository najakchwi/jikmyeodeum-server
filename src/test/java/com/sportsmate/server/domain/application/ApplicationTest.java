package com.sportsmate.server.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Application 도메인 단위 테스트")
class ApplicationTest {

    @Test
    @DisplayName("대기 신청에 상대를 배치하면 23시간 응답 기한이 생성된다")
    void assign_waitingApplication_createsMatchDeadline() {
        Application application = Application.create("ma1", 1L, "g1");

        application.assign(2L);

        assertThat(application.getStatus()).isEqualTo("matched");
        assertThat(application.getMatchedMemberId()).isEqualTo(2L);
        assertThat(application.getExpiresAt()).isEqualTo(application.getMatchedAt().plusHours(23));
    }

    @Test
    @DisplayName("매칭된 신청은 취소할 수 있다")
    void cancel_matchedApplication_changesStatusToCancelled() {
        Application application = Application.create("ma1", 1L, "g1");
        application.assign(2L);

        application.cancel();

        assertThat(application.getStatus()).isEqualTo("cancelled");
    }

    @Test
    @DisplayName("매칭 후보를 대기 상태로 되돌리면 매칭 응답 정보가 초기화된다")
    void resetToWaiting_matchedApplication_clearsMatchFields() {
        Application application = Application.create("ma1", 1L, "g1");
        application.assign(2L);
        application.markAccepted();

        application.resetToWaiting();

        assertThat(application.getStatus()).isEqualTo("waiting");
        assertThat(application.getMatchedMemberId()).isNull();
        assertThat(application.getMatchedAt()).isNull();
        assertThat(application.getExpiresAt()).isNull();
        assertThat(application.getResponse()).isNull();
    }

    @Test
    @DisplayName("양쪽 수락이 확인되면 채팅 상태로 전환한다")
    void confirm_acceptedApplication_opensChat() {
        Application application = Application.create("ma1", 1L, "g1");
        application.assign(2L);
        application.markAccepted();

        application.confirm("chat1");

        assertThat(application.getStatus()).isEqualTo("chatting");
        assertThat(application.getChatId()).isEqualTo("chat1");
    }

    @Test
    @DisplayName("경기 완료 후 평가하면 reviewed 상태가 된다")
    void review_completedGame_changesStatus() {
        Application application = Application.create("ma1", 1L, "g1");
        application.assign(2L);
        application.confirm("chat1");
        application.completeGame();

        application.review();

        assertThat(application.getStatus()).isEqualTo("reviewed");
    }
}
