ALTER TABLE member_preferences
    ALTER COLUMN gender_pref SET DEFAULT 'ANY',
    ALTER COLUMN age_pref SET DEFAULT 'ANY',
    ALTER COLUMN smoking_pref SET DEFAULT 'ANY',
    ALTER COLUMN distance_km SET DEFAULT 5;

UPDATE member_preferences
SET gender_pref = 'ANY'
WHERE gender_pref IS NULL;

UPDATE member_preferences
SET age_pref = 'ANY'
WHERE age_pref IS NULL;

UPDATE member_preferences
SET smoking_pref = 'ANY'
WHERE smoking_pref IS NULL;

UPDATE member_preferences
SET distance_km = 5
WHERE distance_km IS NULL OR distance_km = 0;
