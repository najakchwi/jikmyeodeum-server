package com.sportsmate.server.infrastructure.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.domain.chat.exception.ChatErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatMessageController 단위 테스트")
class ChatMessageControllerTest {

    @Test
    @DisplayName("BusinessException을 REST와 동일한 에러 코드 포맷으로 변환한다")
    void chatErrorPayload_fromBusinessException_usesErrorCodeValue() {
        var payload = ChatMessageController.ChatErrorPayload.from(
                new BusinessException(ChatErrorCode.CHAT_CLOSED));

        assertThat(payload.errorCode()).isEqualTo("C409");
        assertThat(payload.message()).isEqualTo("Chat is closed");
    }
}
