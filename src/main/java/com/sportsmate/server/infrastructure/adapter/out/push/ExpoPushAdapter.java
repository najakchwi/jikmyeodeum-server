package com.sportsmate.server.infrastructure.adapter.out.push;

import com.sportsmate.server.common.port.out.push.PushMessage;
import com.sportsmate.server.common.port.out.push.PushOutPort;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExpoPushAdapter implements PushOutPort {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final RestClient restClient;

    public ExpoPushAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void send(PushMessage message) {
        restClient.post()
                .uri(EXPO_PUSH_URL)
                .body(new ExpoPushRequest(
                        message.to(),
                        message.title(),
                        message.body(),
                        message.data(),
                        "default",
                        1,
                        channelId(message)))
                .retrieve()
                .toBodilessEntity();
    }

    private String channelId(PushMessage message) {
        String type = message.data() == null ? null : message.data().get("type");
        return switch (type == null ? "" : type) {
            case "match" -> "match";
            case "matchSchedule" -> "match-schedule";
            case "chat" -> "chat";
            case "review" -> "review";
            case "marketing" -> "marketing";
            default -> "default";
        };
    }

    private record ExpoPushRequest(
            String to,
            String title,
            String body,
            Map<String, String> data,
            String sound,
            int badge,
            String channelId) {
    }
}
