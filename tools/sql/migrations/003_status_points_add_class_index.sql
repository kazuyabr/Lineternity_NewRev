-- Status Points: Add class_index for subclass support
-- Safe to run: idempotent (IF NOT EXISTS + DROP/ADD PK)

ALTER TABLE character_status_points ADD COLUMN IF NOT EXISTS class_index INT UNSIGNED NOT NULL DEFAULT 0;
ALTER TABLE character_status_points DROP PRIMARY KEY, ADD PRIMARY KEY (char_id, class_index);
