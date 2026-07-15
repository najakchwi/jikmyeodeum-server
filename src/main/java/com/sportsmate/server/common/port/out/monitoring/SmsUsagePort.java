package com.sportsmate.server.common.port.out.monitoring;

public interface SmsUsagePort {
    void recordSent(boolean success);
    void recordRateLimited();
}
