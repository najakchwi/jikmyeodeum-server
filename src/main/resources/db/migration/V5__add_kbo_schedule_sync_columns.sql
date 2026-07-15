ALTER TABLE games
    ADD COLUMN IF NOT EXISTS kbo_game_id VARCHAR(20),
    ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_games_kbo_game_id'
          AND conrelid = 'games'::regclass
    ) THEN
        ALTER TABLE games
            ADD CONSTRAINT uk_games_kbo_game_id UNIQUE (kbo_game_id);
    END IF;
END $$;

ALTER TABLE teams
    ADD COLUMN IF NOT EXISTS kbo_code VARCHAR(20);

ALTER TABLE stadiums
    ADD COLUMN IF NOT EXISTS kbo_code VARCHAR(20);

UPDATE teams
SET kbo_code = short_name
WHERE kbo_code IS NULL;

UPDATE stadiums
SET kbo_code = name
WHERE kbo_code IS NULL;
