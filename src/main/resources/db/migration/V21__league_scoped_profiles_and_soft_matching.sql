ALTER TABLE teams
    ADD COLUMN IF NOT EXISTS league_id BIGINT;

UPDATE teams
SET league_id = 1
WHERE league_id IS NULL;

ALTER TABLE teams
    ALTER COLUMN league_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_teams_league') THEN
        ALTER TABLE teams ADD CONSTRAINT fk_teams_league FOREIGN KEY (league_id) REFERENCES leagues (id);
    END IF;
END $$;

ALTER TABLE member_styles
    ADD COLUMN IF NOT EXISTS drinking_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS meet_purpose VARCHAR(50);

ALTER TABLE member_preferences
    ADD COLUMN IF NOT EXISTS drinking_pref VARCHAR(20) DEFAULT 'ANY',
    ADD COLUMN IF NOT EXISTS talk_pref VARCHAR(20) DEFAULT 'ANY',
    ADD COLUMN IF NOT EXISTS fan_level_pref VARCHAR(20) DEFAULT 'ANY';

UPDATE member_preferences
SET drinking_pref = COALESCE(drinking_pref, 'ANY'),
    talk_pref = COALESCE(talk_pref, 'ANY'),
    fan_level_pref = COALESCE(fan_level_pref, 'ANY');

CREATE TABLE IF NOT EXISTS member_league_profiles (
    member_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,
    favorite_team_id BIGINT,
    team_pref VARCHAR(20),
    fan_level VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (member_id, league_id),
    CONSTRAINT fk_member_league_profiles_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_member_league_profiles_league FOREIGN KEY (league_id) REFERENCES leagues (id),
    CONSTRAINT fk_member_league_profiles_team FOREIGN KEY (favorite_team_id) REFERENCES teams (id)
);

CREATE TABLE IF NOT EXISTS member_league_watch_styles (
    member_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,
    watch_style VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (member_id, league_id, watch_style),
    CONSTRAINT fk_member_league_watch_styles_profile FOREIGN KEY (member_id, league_id)
        REFERENCES member_league_profiles (member_id, league_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS member_league_seat_zones (
    member_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,
    seat_zone VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (member_id, league_id, seat_zone),
    CONSTRAINT fk_member_league_seat_zones_profile FOREIGN KEY (member_id, league_id)
        REFERENCES member_league_profiles (member_id, league_id) ON DELETE CASCADE
);

INSERT INTO member_league_profiles (member_id, league_id, favorite_team_id, team_pref, fan_level)
SELECT member_id, 1, favorite_team_id, 'ANY', NULL
FROM member_styles
ON CONFLICT (member_id, league_id) DO NOTHING;

INSERT INTO member_league_watch_styles (member_id, league_id, watch_style)
SELECT mws.member_id, 1, mws.watch_style
FROM member_watch_styles mws
JOIN member_league_profiles mlp
  ON mlp.member_id = mws.member_id AND mlp.league_id = 1
ON CONFLICT DO NOTHING;
