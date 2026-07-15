package com.sportsmate.server.common.port.out.push;

import java.util.Map;

public record PushMessage(
        String to,
        String title,
        String body,
        Map<String, String> data) {
}
