-- Status Points v3: Two-pool system (Attribute + Direct Status)
-- Run this migration to create the character_status_points table

CREATE TABLE IF NOT EXISTS character_status_points (
  char_id INT UNSIGNED NOT NULL,
  class_index INT UNSIGNED NOT NULL DEFAULT 0,
  
  -- Attribute pool (STR/CON/DEX/INT/WIT/MEN)
  attr_available INT UNSIGNED DEFAULT 0,
  attr_distributed INT UNSIGNED DEFAULT 0,
  attr_str INT UNSIGNED DEFAULT 0,
  attr_con INT UNSIGNED DEFAULT 0,
  attr_dex INT UNSIGNED DEFAULT 0,
  attr_int INT UNSIGNED DEFAULT 0,
  attr_wit INT UNSIGNED DEFAULT 0,
  attr_men INT UNSIGNED DEFAULT 0,
  
  -- Direct status pool (P.Def/M.Def/HP/MP/CP/P.Atk/M.Atk/Acc/Eva/Crit)
  status_available INT UNSIGNED DEFAULT 0,
  status_distributed INT UNSIGNED DEFAULT 0,
  status_pdef INT UNSIGNED DEFAULT 0,
  status_mdef INT UNSIGNED DEFAULT 0,
  status_hp INT UNSIGNED DEFAULT 0,
  status_mp INT UNSIGNED DEFAULT 0,
  status_cp INT UNSIGNED DEFAULT 0,
  status_patk INT UNSIGNED DEFAULT 0,
  status_matk INT UNSIGNED DEFAULT 0,
  status_accuracy INT UNSIGNED DEFAULT 0,
  status_evasion INT UNSIGNED DEFAULT 0,
  status_crit INT UNSIGNED DEFAULT 0,
  
  -- Karma penalty (attribute pool only)
  karma_penalty_attr INT UNSIGNED DEFAULT 0,
  
  -- Source tracking
  source_level_points INT UNSIGNED DEFAULT 0,
  source_quest_points INT UNSIGNED DEFAULT 0,
  source_raid_points INT UNSIGNED DEFAULT 0,
  source_siege_points INT UNSIGNED DEFAULT 0,
  source_pvp_points INT UNSIGNED DEFAULT 0,
  source_karma_points INT UNSIGNED DEFAULT 0,
  
  -- Meta
  version INT UNSIGNED DEFAULT 1,
  is_old_char TINYINT DEFAULT 0,
  reset_date DATETIME DEFAULT NULL,
  
  PRIMARY KEY (char_id, class_index)
) ENGINE=InnoDB;
