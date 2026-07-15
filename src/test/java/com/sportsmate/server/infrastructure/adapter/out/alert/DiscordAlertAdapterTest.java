package com.sportsmate.server.infrastructure.adapter.out.alert;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("DiscordAlertAdapter 단위 테스트")
class DiscordAlertAdapterTest {

    @Test
    @DisplayName("웹훅이 미설정된 심각도는 외부 요청 없이 no-op 처리한다")
    void notify_withoutWebhook_noops() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = adapter(builder.build(), "");

        adapter.notify(AlertSeverity.WARNING, AlertMessage.of("title", "summary", "key"));

        server.verify();
    }

    @Test
    @DisplayName("동일 dedupeKey는 TTL 안에서 한 번만 발송한다")
    void notify_sameDedupeKey_debounces() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://discord.test/warning")).andRespond(withSuccess());
        var adapter = adapter(builder.build(), "https://discord.test/warning");

        adapter.notify(AlertSeverity.WARNING, AlertMessage.of("title", "summary", "key"));
        adapter.notify(AlertSeverity.WARNING, AlertMessage.of("title", "summary", "key"));

        server.verify();
    }

    @Test
    @DisplayName("resolve는 미해결 dedupeKey에 대해 복구 알림을 한 번 발송한다")
    void resolve_withUnresolvedKey_sendsRecovery() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://discord.test/warning")).andRespond(withSuccess());
        server.expect(once(), requestTo("https://discord.test/warning")).andRespond(withSuccess());
        var adapter = adapter(builder.build(), "https://discord.test/warning");

        adapter.notify(AlertSeverity.WARNING, new AlertMessage("title", "summary", Map.of("a", "b"), "key"));
        adapter.resolve("key");
        adapter.resolve("key");

        server.verify();
    }

    private DiscordAlertAdapter adapter(RestClient restClient, String webhookUrl) {
        return new DiscordAlertAdapter(
                restClient,
                new AlertDebounceStore(),
                new MockEnvironment().withProperty("spring.profiles.active", "test"),
                "",
                webhookUrl,
                webhookUrl,
                webhookUrl,
                Duration.ofMinutes(15),
                "server");
    }
}
