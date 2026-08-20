-- Migration: Add ct (creation timestamp) to characters table
-- Required for StatusPointActivationDate feature
-- Run this if your characters table was created before this migration

ALTER TABLE characters ADD COLUMN IF NOT EXISTS ct INT(11) NOT NULL DEFAULT 0 COMMENT 'Character creation timestamp';
