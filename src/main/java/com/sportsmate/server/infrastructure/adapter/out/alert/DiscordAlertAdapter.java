package com.sportsmate.server.infrastructure.adapter.out.alert;

import com.sportsmate.server.common.port.out.alert.AlertMessage;
import com.sportsmate.server.common.port.out.alert.AlertSeverity;
import com.sportsmate.server.common.port.out.alert.OpsAlertPort;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("prod")
public class DiscordAlertAdapter implements OpsAlertPort {

    private static final Logger log = LoggerFactory.getLogger(DiscordAlertAdapter.class);
    private static final int DESCRIPTION_LIMIT = 3900;

    private final RestClient restClient;
    private final AlertDebounceStore debounceStore;
    private final Map<AlertSeverity, String> webhookUrls;
    private final Map<AlertSeverity, Boolean> missingWebhookLogged = new ConcurrentHashMap<>();
    private final Duration debounceTtl;
    private final String environmentName;
    private final String instanceName;

    public DiscordAlertAdapter(
            RestClient restClient,
            AlertDebounceStore debounceStore,
            Environment environment,
            @Value("${app.discord.webhooks.critical:}") String criticalWebhookUrl,
            @Value("${app.discord.webhooks.warning:}") String warningWebhookUrl,
            @Value("${app.discord.webhooks.info:}") String infoWebhookUrl,
            @Value("${app.discord.webhooks.metrics:}") String metricsWebhookUrl,
            @Value("${app.discord.debounce-ttl:PT15M}") Duration debounceTtl,
            @Value("${spring.application.name:server}") String instanceName) {
        this.restClient = restClient;
        this.debounceStore = debounceStore;
        this.webhookUrls = new EnumMap<>(AlertSeverity.class);
        this.webhookUrls.put(AlertSeverity.CRITICAL, criticalWebhookUrl);
        this.webhookUrls.put(AlertSeverity.WARNING, warningWebhookUrl);
        this.webhookUrls.put(AlertSeverity.INFO, infoWebhookUrl);
        this.webhookUrls.put(AlertSeverity.METRIC, metricsWebhookUrl);
        this.debounceTtl = debounceTtl;
        this.environmentName = String.join(",", environment.getActiveProfiles());
        this.instanceName = instanceName;
    }

    @Override
    public void notify(AlertSeverity severity, AlertMessage message) {
        try {
            String webhookUrl = webhookUrls.get(severity);
            if (webhookUrl == null || webhookUrl.isBlank()) {
                logMissingWebhookOnce(severity);
                return;
            }
            if (!debounceStore.shouldSend(message.dedupeKey(), debounceTtl)) {
                return;
            }
            restClient.post()
                    .uri(webhookUrl)
                    .body(payload(severity, message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("Discord alert delivery failed. severity={}, title={}", severity, message.title(), exception);
        }
    }

    @Override
    public void resolve(String dedupeKey) {
        if (!debounceStore.markResolved(dedupeKey)) {
            return;
        }
        notify(AlertSeverity.INFO, new AlertMessage(
                "복구됨",
                "이전 알림 상태가 정상으로 복구되었습니다.",
                Map.of("dedupeKey", dedupeKey),
                null));
    }

    private Map<String, Object> payload(AlertSeverity severity, AlertMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (severity == AlertSeverity.CRITICAL) {
            payload.put("content", "@here");
        }
        payload.put("embeds", List.of(embed(severity, message)));
        return payload;
    }

    private Map<String, Object> embed(AlertSeverity severity, AlertMessage message) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", icon(severity) + " " + message.title());
        embed.put("description", truncate(message.summary()));
        embed.put("color", color(severity));
        embed.put("timestamp", Instant.now().toString());
        embed.put("footer", Map.of("text", footer()));
        List<Map<String, Object>> fields = new ArrayList<>();
        message.fields().forEach((name, value) -> fields.add(Map.of(
                "name", name,
                "value", value == null || value.isBlank() ? "-" : value,
                "inline", true)));
        if (!fields.isEmpty()) {
            embed.put("fields", fields);
        }
        return embed;
    }

    private void logMissingWebhookOnce(AlertSeverity severity) {
        if (missingWebhookLogged.putIfAbsent(severity, true) == null) {
            log.warn("Discord webhook is not configured. severity={}", severity);
        }
    }

    private String footer() {
        String profile = environmentName == null || environmentName.isBlank() ? "default" : environmentName;
        return profile + " / " + instanceName;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= DESCRIPTION_LIMIT ? value : value.substring(0, DESCRIPTION_LIMIT) + "...";
    }

    private String icon(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "🔴";
            case WARNING -> "🟡";
            case INFO -> "🔵";
            case METRIC -> "📊";
        };
    }

    private int color(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 15158332;
            case WARNING -> 16776960;
            case INFO -> 3447003;
            case METRIC -> 3066993;
        };
    }
}
