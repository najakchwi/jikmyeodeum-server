ALTER TABLE members
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE auth
    ADD COLUMN IF NOT EXISTS member_id BIGINT;

UPDATE members m
SET phone = a.phone,
    phone_verified_at = COALESCE(m.phone_verified_at, CURRENT_TIMESTAMP)
FROM auth a
WHERE m.auth_id = a.id
  AND a.phone IS NOT NULL
  AND m.phone IS NULL;

UPDATE auth a
SET member_id = m.id
FROM members m
WHERE m.auth_id = a.id
  AND a.member_id IS NULL;

ALTER TABLE members
    ALTER COLUMN auth_id DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_auth_member'
          AND conrelid = 'auth'::regclass
    ) THEN
        ALTER TABLE auth
            ADD CONSTRAINT fk_auth_member
                FOREIGN KEY (member_id) REFERENCES members(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_members_phone_active
    ON members(phone)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS member_withdrawal_logs (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    phone VARCHAR(20) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    reason_detail VARCHAR(200),
    withdrawn_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_member_login_type
    ON auth(member_id, login_type)
    WHERE member_id IS NOT NULL AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_auth_provider
    ON auth(login_type, provider_id)
    WHERE provider_id IS NOT NULL AND status = 'ACTIVE';
