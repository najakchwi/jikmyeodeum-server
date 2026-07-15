CREATE TABLE IF NOT EXISTS phone_change_logs (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    old_phone VARCHAR(20) NOT NULL,
    new_phone VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_phone_change_logs_member_changed_at
    ON phone_change_logs(member_id, changed_at DESC);
