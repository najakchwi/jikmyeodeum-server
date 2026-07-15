package com.sportsmate.server.infrastructure.adapter.out.storage.log;

public enum LogType {
    AUDIT("audit"),
    API("api");

    private final String filePrefix;

    LogType(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    public String filePrefix() {
        return filePrefix;
    }
}
