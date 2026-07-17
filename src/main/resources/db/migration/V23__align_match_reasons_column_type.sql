ALTER TABLE match_applications
    ALTER COLUMN match_reasons TYPE TEXT
    USING match_reasons::text;