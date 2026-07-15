package com.sportsmate.server.common.port.out.location;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LocationRegion(
        String depth1,
        String depth2,
        String depth3
) {
    public String toAddress() {
        return Stream.of(depth1, depth2, depth3)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }
}
