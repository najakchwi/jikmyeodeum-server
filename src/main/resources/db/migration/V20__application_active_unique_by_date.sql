ALTER TABLE match_applications ADD COLUMN IF NOT EXISTS game_date DATE;

UPDATE match_applications a
SET game_date = g.date
FROM games g
WHERE g.id = a.game_id
  AND a.game_date IS NULL;

ALTER TABLE match_applications ALTER COLUMN game_date SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_match_applications_active_member_date
    ON match_applications(member_id, game_date)
    WHERE status <> 'cancelled';
