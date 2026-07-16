ALTER TABLE match_applications
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS ux_match_applications_active_member_game
    ON match_applications(member_id, game_id)
    WHERE status <> 'cancelled';
