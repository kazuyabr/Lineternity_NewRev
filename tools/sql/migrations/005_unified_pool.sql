-- Migration 005: Status Points v4 - Unified pool + exponential cost
-- Migrates legacy attr_available/status_available columns into the unified
-- `available` pool. Guarded by information_schema so it is safe to run any
-- number of times (fresh installs from the consolidated base schema are no-op).

DROP PROCEDURE IF EXISTS sp_migration_005;

DELIMITER $$
CREATE PROCEDURE sp_migration_005()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'character_status_points'
          AND COLUMN_NAME = 'attr_available'
    ) THEN
        ALTER TABLE character_status_points
            ADD COLUMN IF NOT EXISTS `available` INT UNSIGNED DEFAULT 0 AFTER `class_index`;

        UPDATE character_status_points SET
            `available` = IFNULL(attr_available, 0) + IFNULL(status_available, 0) + IFNULL(attr_distributed, 0) + IFNULL(status_distributed, 0),
            attr_str = 0, attr_con = 0, attr_dex = 0, attr_int = 0, attr_wit = 0, attr_men = 0,
            attr_distributed = 0,
            status_pdef = 0, status_mdef = 0, status_hp = 0, status_mp = 0, status_cp = 0,
            status_patk = 0, status_matk = 0, status_accuracy = 0, status_evasion = 0, status_crit = 0,
            status_distributed = 0;

        ALTER TABLE character_status_points
            DROP COLUMN `attr_available`,
            DROP COLUMN `status_available`;
    END IF;
END$$
DELIMITER ;

CALL sp_migration_005();

DROP PROCEDURE IF EXISTS sp_migration_005;
