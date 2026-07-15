DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_teams_short_name') THEN
        ALTER TABLE teams ADD CONSTRAINT uk_teams_short_name UNIQUE (short_name);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_teams_kbo_code') THEN
        ALTER TABLE teams ADD CONSTRAINT uk_teams_kbo_code UNIQUE (kbo_code);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_stadiums_kbo_code') THEN
        ALTER TABLE stadiums ADD CONSTRAINT uk_stadiums_kbo_code UNIQUE (kbo_code);
    END IF;
END $$;
