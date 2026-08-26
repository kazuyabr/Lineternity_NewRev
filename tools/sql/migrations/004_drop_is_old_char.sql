-- Remove is_old_char column (now computed at runtime from version + createTime)
-- Safe to run: uses IF EXISTS

ALTER TABLE character_status_points DROP COLUMN IF EXISTS is_old_char;
