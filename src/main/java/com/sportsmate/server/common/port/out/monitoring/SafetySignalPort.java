package com.sportsmate.server.common.port.out.monitoring;

public interface SafetySignalPort {
    void recordReport();
    void recordWithdrawal();
}
