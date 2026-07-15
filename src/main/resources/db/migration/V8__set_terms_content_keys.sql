UPDATE terms
SET content_key = 'terms/service/2026-06-21.txt'
WHERE code = 'service'
  AND version = '2026-06-21'
  AND content_key IS NULL;

UPDATE terms
SET content_key = 'terms/privacy/2026-06-21.txt'
WHERE code = 'privacy'
  AND version = '2026-06-21'
  AND content_key IS NULL;

UPDATE terms
SET content_key = 'terms/location/2026-06-21.txt'
WHERE code = 'location'
  AND version = '2026-06-21'
  AND content_key IS NULL;
