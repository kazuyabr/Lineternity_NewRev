-- Migration 006: Status Points - chest reward source tracking
-- Adds the source_chest_points column used by the chest opening reward system.

ALTER TABLE character_status_points ADD COLUMN IF NOT EXISTS source_chest_points INT UNSIGNED NOT NULL DEFAULT 0;
