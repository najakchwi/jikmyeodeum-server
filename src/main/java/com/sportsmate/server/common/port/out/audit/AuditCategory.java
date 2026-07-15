package com.sportsmate.server.common.port.out.audit;

public enum AuditCategory {
    AUTH_LOGIN(false),
    ADMIN_ACTION(true),
    TRUST_SCORE(true),
    CONSENT(true),
    REPORT(true),
    CONTENT_MANAGEMENT(false);

    private final boolean persisted;

    AuditCategory(boolean persisted) {
        this.persisted = persisted;
    }

    public boolean isPersisted() {
        return persisted;
    }
}
