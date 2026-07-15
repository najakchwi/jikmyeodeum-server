CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(30) NOT NULL,
    action VARCHAR(100) NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_id VARCHAR(50),
    target_type VARCHAR(30),
    target_id VARCHAR(50),
    result VARCHAR(10) NOT NULL,
    detail TEXT,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_category_occurred_at ON audit_logs (category, occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_target ON audit_logs (target_type, target_id);
