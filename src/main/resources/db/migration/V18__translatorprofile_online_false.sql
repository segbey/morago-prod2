-- ALTER TABLE translator_profiles
--     MODIFY COLUMN is_online    BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE translator_profiles
SET is_online = 0
WHERE is_online IS NULL;

ALTER TABLE translator_profiles
    MODIFY COLUMN is_online TINYINT(1) NOT NULL DEFAULT 0;