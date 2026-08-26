-- Migration 007: Status Points - drop orphaned reset_date column
-- The column is not referenced by any Java code (legacy from an older iteration).

ALTER TABLE character_status_points DROP COLUMN IF EXISTS reset_date;
