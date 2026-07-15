UPDATE games
SET deadline = date - 1
WHERE deadline <> date - 1;
