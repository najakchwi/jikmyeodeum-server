ALTER TABLE match_applications
    ADD COLUMN IF NOT EXISTS match_score INTEGER;

CREATE TABLE IF NOT EXISTS match_application_rejected_members (
    application_id BIGINT NOT NULL,
    rejected_member_id BIGINT NOT NULL,
    CONSTRAINT fk_match_application_rejected_members_application
        FOREIGN KEY (application_id) REFERENCES match_applications(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_match_application_rejected_members
    ON match_application_rejected_members(application_id, rejected_member_id);
