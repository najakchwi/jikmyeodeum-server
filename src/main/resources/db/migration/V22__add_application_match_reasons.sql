ALTER TABLE match_applications
    ADD COLUMN IF NOT EXISTS match_reasons JSON;
