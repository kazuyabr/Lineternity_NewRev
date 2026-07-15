-- ============================================================
-- Tabelas de GameServer (GameServer Database)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- Tabelas de Personagens
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `characters` (
  `account_name` varchar(45) DEFAULT NULL,
  `char_id` int(11) NOT NULL DEFAULT 0,
  `char_name` varchar(35) NOT NULL DEFAULT '',
  `level` int(4) NOT NULL DEFAULT 0,
  `exp` bigint(20) NOT NULL DEFAULT 0,
  `sp` int(11) NOT NULL DEFAULT 0,
  `curHp` double NOT NULL DEFAULT 1,
  `curMp` double NOT NULL DEFAULT 1,
  `curCp` double NOT NULL DEFAULT 1,
  `maxHp` int(11) NOT NULL DEFAULT 1,
  `maxMp` int(11) NOT NULL DEFAULT 1,
  `maxCp` int(11) NOT NULL DEFAULT 1,
  `face` int(3) NOT NULL DEFAULT 0,
  `hairStyle` int(2) NOT NULL DEFAULT 0,
  `hairColor` int(3) NOT NULL DEFAULT 0,
  `sex` int(1) NOT NULL DEFAULT 0,
  `heading` int(11) NOT NULL DEFAULT 0,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `accesslevel` int(2) NOT NULL DEFAULT 0,
  `online` int(1) NOT NULL DEFAULT 0,
  `onlinetime` int(11) NOT NULL DEFAULT 0,
  `lastAccess` int(11) NOT NULL DEFAULT 0,
  `clanid` int(11) NOT NULL DEFAULT 0,
  `clan_privileges` int(11) NOT NULL DEFAULT 0,
  `race` tinyint(1) NOT NULL DEFAULT 0,
  `class_id` tinyint(2) NOT NULL DEFAULT 0,
  `base_class` tinyint(2) NOT NULL DEFAULT 0,
  `transform_id` int(11) NOT NULL DEFAULT 0,
  `land` int(11) NOT NULL DEFAULT 0,
  `fame` int(11) NOT NULL DEFAULT 0,
  `pvp_flag` int(1) NOT NULL DEFAULT 0,
  `pvpkills` int(11) NOT NULL DEFAULT 0,
  `pkkills` int(11) NOT NULL DEFAULT 0,
  `rec_have` int(3) NOT NULL DEFAULT 0,
  `rec_left` int(3) NOT NULL DEFAULT 0,
  `ct` int(11) NOT NULL DEFAULT 0,
  `char_type` int(1) NOT NULL DEFAULT 0,
  `novice` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`),
  KEY `account_name` (`account_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_data` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `key` varchar(46) NOT NULL DEFAULT '',
  `value` text,
  PRIMARY KEY (`char_id`,`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_hennas` (
  `obj_id` int(10) NOT NULL DEFAULT 0,
  `symbol_id` int(11) NOT NULL DEFAULT 0,
  `dye_level` int(11) NOT NULL DEFAULT 0,
  `dye_price` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`obj_id`,`symbol_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_macroses` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `id` int(2) NOT NULL DEFAULT 0,
  `name` varchar(40) DEFAULT NULL,
  `desc` varchar(200) DEFAULT NULL,
  `acronym` varchar(20) DEFAULT NULL,
  `icon` int(11) DEFAULT NULL,
  PRIMARY KEY (`char_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_memo` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `slot` int(11) NOT NULL DEFAULT 0,
  `name` varchar(255) DEFAULT NULL,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `tail` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`char_id`,`slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_mission` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `mission_id` int(11) NOT NULL DEFAULT 0,
  `mission_state` int(11) NOT NULL DEFAULT 0,
  `mission_count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`mission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_quests` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `quest_id` int(11) NOT NULL DEFAULT 0,
  `quest_state` int(11) NOT NULL DEFAULT 0,
  `explored_count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`quest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_raid_points` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `boss_id` int(11) NOT NULL DEFAULT 0,
  `points` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`boss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_recipebook` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `id` int(11) NOT NULL DEFAULT 0,
  `sp` int(11) NOT NULL DEFAULT 0,
  `count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_recommends` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `target_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_relations` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `target_id` int(11) NOT NULL DEFAULT 0,
  `relation` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_shortcuts` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `slot` int(11) NOT NULL DEFAULT 0,
  `page` int(11) NOT NULL DEFAULT 0,
  `type` int(11) NOT NULL DEFAULT 0,
  `shortcut_id` int(11) NOT NULL DEFAULT 0,
  `level` int(11) NOT NULL DEFAULT 0,
  `sublevel` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`slot`,`page`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_skills` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `skill_id` int(11) NOT NULL DEFAULT 0,
  `skill_level` int(3) NOT NULL DEFAULT 0,
  `skill_cp` int(11) NOT NULL DEFAULT 0,
  `skill_hp` int(11) NOT NULL DEFAULT 0,
  `sp` int(11) NOT NULL DEFAULT 0,
  `timer` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_skills_save` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `skill_id` int(11) NOT NULL DEFAULT 0,
  `skill_level` int(3) NOT NULL DEFAULT 0,
  `skill_cp` int(11) NOT NULL DEFAULT 0,
  `skill_hp` int(11) NOT NULL DEFAULT 0,
  `sp` int(11) NOT NULL DEFAULT 0,
  `timer` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `character_subclasses` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `class_index` int(11) NOT NULL DEFAULT 0,
  `class_id` int(11) NOT NULL DEFAULT 0,
  `exp` bigint(20) NOT NULL DEFAULT 0,
  `sp` int(11) NOT NULL DEFAULT 0,
  `level` int(4) NOT NULL DEFAULT 1,
  `curHp` double NOT NULL DEFAULT 1,
  `curMp` double NOT NULL DEFAULT 1,
  `curCp` double NOT NULL DEFAULT 1,
  `base_class` int(11) NOT NULL DEFAULT 0,
  `death_penalty_count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`class_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Itens
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `items` (
  `owner_id` int(11) NOT NULL DEFAULT 0,
  `object_id` int(11) NOT NULL DEFAULT 0,
  `item_id` int(11) NOT NULL DEFAULT 0,
  `count` bigint(20) NOT NULL DEFAULT 0,
  `enchant_level` int(11) NOT NULL DEFAULT 0,
  `location` varchar(25) NOT NULL DEFAULT '',
  `slot` int(11) NOT NULL DEFAULT 0,
  `custom_type1` int(11) NOT NULL DEFAULT 0,
  `custom_type2` int(11) NOT NULL DEFAULT 0,
  `mana_left` int(11) NOT NULL DEFAULT -1,
  `time` int(11) NOT NULL DEFAULT -1,
  `visual_item_id` int(11) NOT NULL DEFAULT 0,
  `var_attributes_a` int(11) NOT NULL DEFAULT 0,
  `var_attributes_b` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`object_id`),
  KEY `owner_id` (`owner_id`),
  KEY `item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `items_on_ground` (
  `object_id` int(11) NOT NULL DEFAULT 0,
  `item_id` int(11) NOT NULL DEFAULT 0,
  `count` int(11) NOT NULL DEFAULT 0,
  `enchant_level` int(11) NOT NULL DEFAULT 0,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `drop_time` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Clãs
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `clan_data` (
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `clan_name` varchar(45) NOT NULL DEFAULT '',
  `leader_id` int(11) NOT NULL DEFAULT 0,
  `leader_name` varchar(35) NOT NULL DEFAULT '',
  `ally_id` int(11) NOT NULL DEFAULT 0,
  `ally_name` varchar(45) NOT NULL DEFAULT '',
  `crest_id` int(11) NOT NULL DEFAULT 0,
  `ally_crest_id` int(11) NOT NULL DEFAULT 0,
  `level` int(2) NOT NULL DEFAULT 0,
  `rep_score` int(11) NOT NULL DEFAULT 0,
  `warn_time` bigint(20) NOT NULL DEFAULT 0,
  `not_ally_penalty` bigint(20) NOT NULL DEFAULT 0,
  `dissolving_time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clan_privs` (
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `rank` int(2) NOT NULL DEFAULT 0,
  `privs` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clan_id`,`rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clan_skills` (
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `skill_id` int(11) NOT NULL DEFAULT 0,
  `skill_level` int(3) NOT NULL DEFAULT 0,
  `skill_cp` int(11) NOT NULL DEFAULT 0,
  `skill_hp` int(11) NOT NULL DEFAULT 0,
  `sp` int(11) NOT NULL DEFAULT 0,
  `timer` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clan_id`,`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clan_subpledges` (
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `subpledge_id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(45) NOT NULL DEFAULT '',
  `leader_id` int(11) NOT NULL DEFAULT 0,
  `type` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clan_id`,`subpledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clan_wars` (
  `clan1_id` int(11) NOT NULL DEFAULT 0,
  `clan2_id` int(11) NOT NULL DEFAULT 0,
  `time` bigint(20) NOT NULL DEFAULT 0,
  `winner_clan` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clan1_id`,`clan2_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Castelos e Clanhalls
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `castle` (
  `id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(25) NOT NULL DEFAULT '',
  `owner_id` int(11) NOT NULL DEFAULT 0,
  `tax_percent` int(11) NOT NULL DEFAULT 0,
  `treasury` bigint(20) NOT NULL DEFAULT 0,
  `show_nextharvest` int(1) NOT NULL DEFAULT 0,
  `show_nextriod` int(1) NOT NULL DEFAULT 0,
  `last_siege_time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `castle_doorupgrade` (
  `door_id` int(11) NOT NULL DEFAULT 0,
  `castle_id` int(11) NOT NULL DEFAULT 0,
  `hp` int(11) NOT NULL DEFAULT 0,
  `pDef` int(11) NOT NULL DEFAULT 0,
  `mDef` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`door_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `castle_functions` (
  `castle_id` int(11) NOT NULL DEFAULT 0,
  `type` int(11) NOT NULL DEFAULT 0,
  `lvl` int(11) NOT NULL DEFAULT 0,
  `leveled` int(1) NOT NULL DEFAULT 0,
  `price` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`castle_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `castle_manor_procure` (
  `castle_id` int(11) NOT NULL DEFAULT 0,
  `crop_id` int(11) NOT NULL DEFAULT 0,
  `target_id` int(11) NOT NULL DEFAULT 0,
  `amount` bigint(20) NOT NULL DEFAULT 0,
  `price` int(11) NOT NULL DEFAULT 0,
  `manor_cost` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`castle_id`,`crop_id`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `castle_manor_production` (
  `castle_id` int(11) NOT NULL DEFAULT 0,
  `crop_id` int(11) NOT NULL DEFAULT 0,
  `amount` bigint(20) NOT NULL DEFAULT 0,
  `start_level` int(11) NOT NULL DEFAULT 0,
  `end_level` int(11) NOT NULL DEFAULT 0,
  `start_amount` bigint(20) NOT NULL DEFAULT 0,
  `end_amount` bigint(20) NOT NULL DEFAULT 0,
  `next_cycle` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`castle_id`,`crop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `castle_trapupgrade` (
  `castle_id` int(11) NOT NULL DEFAULT 0,
  `tower_index` int(11) NOT NULL DEFAULT 0,
  `level` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`castle_id`,`tower_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clanhall` (
  `id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(45) NOT NULL DEFAULT '',
  `owner_id` int(11) NOT NULL DEFAULT 0,
  `location` varchar(45) NOT NULL DEFAULT '',
  `length` int(11) NOT NULL DEFAULT 0,
  `width` int(11) NOT NULL DEFAULT 0,
  `height` int(11) NOT NULL DEFAULT 0,
  `cost_e` int(11) NOT NULL DEFAULT 0,
  `cost_l` int(11) NOT NULL DEFAULT 0,
  `grade` int(1) NOT NULL DEFAULT 0,
  `previous_owner` int(11) NOT NULL DEFAULT 0,
  `paid` int(5) NOT NULL DEFAULT 0,
  `desc` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clanhall_functions` (
  `clanhall_id` int(11) NOT NULL DEFAULT 0,
  `type` int(11) NOT NULL DEFAULT 0,
  `lvl` int(11) NOT NULL DEFAULT 0,
  `leveled` int(1) NOT NULL DEFAULT 0,
  `price` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clanhall_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clanhall_flagwar_attackers` (
  `clanhall_id` int(11) NOT NULL DEFAULT 0,
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `flag_alive` int(1) NOT NULL DEFAULT 0,
  `time_remaining` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clanhall_id`,`clan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clanhall_flagwar_members` (
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `flag_npc_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clan_id`,`flag_npc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clanhall_flagwar_owner_npcs` (
  `clanhall_id` int(11) NOT NULL DEFAULT 0,
  `npc_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clanhall_id`,`npc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `clanhall_siege_attackers` (
  `clanhall_id` int(11) NOT NULL DEFAULT 0,
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `time_remaining` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`clanhall_id`,`clan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Sieges
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `siege_clans` (
  `castle_id` int(11) NOT NULL DEFAULT 0,
  `clan_id` int(11) NOT NULL DEFAULT 0,
  `type` int(11) NOT NULL DEFAULT 0,
  `side` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`castle_id`,`clan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Spawn
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `spawn_data` (
  `id` int(11) NOT NULL DEFAULT 0,
  `group_id` int(11) NOT NULL DEFAULT 0,
  `template_id` int(11) NOT NULL DEFAULT 0,
  `count` int(11) NOT NULL DEFAULT 0,
  `location` varchar(200) NOT NULL DEFAULT '',
  `pole_type` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Grand Boss
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `grandboss_list` (
  `boss_id` int(11) NOT NULL DEFAULT 0,
  `respawn_time` bigint(20) NOT NULL DEFAULT 0,
  `respawn_date` bigint(20) NOT NULL DEFAULT 0,
  `hp` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`boss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Seven Signs
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `seven_signs` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `dawn_stone_count` int(11) NOT NULL DEFAULT 0,
  `dawn_festival_count` int(11) NOT NULL DEFAULT 0,
  `dusk_stone_count` int(11) NOT NULL DEFAULT 0,
  `dusk_festival_count` int(11) NOT NULL DEFAULT 0,
  `ancient_adena_count` int(11) NOT NULL DEFAULT 0,
  `conflict` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `seven_signs_festival` (
  `oracle_seal` int(11) NOT NULL DEFAULT 0,
  `festival_cycle` int(11) NOT NULL DEFAULT 0,
  `festival_number` int(11) NOT NULL DEFAULT 0,
  `accumulated_bonus` int(11) NOT NULL DEFAULT 0,
  `festival_cycle_start` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`oracle_seal`,`festival_cycle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `seven_signs_status` (
  `id` int(11) NOT NULL DEFAULT 0,
  `current_period` int(11) NOT NULL DEFAULT 0,
  `accumulated_bonus_noah` int(11) NOT NULL DEFAULT 0,
  `accumulated_bonus_spiritstone` int(11) NOT NULL DEFAULT 0,
  `accumulated_bonus_festival` int(11) NOT NULL DEFAULT 0,
  `previous_winner` int(11) NOT NULL DEFAULT 0,
  `aof_scarlet_score` int(11) NOT NULL DEFAULT 0,
  `aof_dawn_score` int(11) NOT NULL DEFAULT 0,
  `aof_undying_score` int(11) NOT NULL DEFAULT 0,
  `aof_dusk_score` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Olympiad
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `olympiad_data` (
  `id` int(11) NOT NULL DEFAULT 0,
  `current_cycle` int(11) NOT NULL DEFAULT 0,
  `period` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `olympiad_fights` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `target_id` int(11) NOT NULL DEFAULT 0,
  `start_time` bigint(20) NOT NULL DEFAULT 0,
  `time` bigint(20) NOT NULL DEFAULT 0,
  `class` int(11) NOT NULL DEFAULT 0,
  `damage` int(11) NOT NULL DEFAULT 0,
  `hp` int(11) NOT NULL DEFAULT 0,
  `result` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`target_id`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `olympiad_nobles` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `class_id` int(11) NOT NULL DEFAULT 0,
  `char_name` varchar(35) NOT NULL DEFAULT '',
  `olympiad_points` int(11) NOT NULL DEFAULT 80,
  `competitions_done` int(11) NOT NULL DEFAULT 0,
  `competitions_won` int(11) NOT NULL DEFAULT 0,
  `competitions_lost` int(11) NOT NULL DEFAULT 0,
  `competitions_drawn` int(11) NOT NULL DEFAULT 0,
  `old_historical_points` int(11) NOT NULL DEFAULT 80,
  `bonus` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `olympiad_nobles_eom` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `class_id` int(11) NOT NULL DEFAULT 0,
  `char_name` varchar(35) NOT NULL DEFAULT '',
  `olympiad_points` int(11) NOT NULL DEFAULT 0,
  `competitions_done` int(11) NOT NULL DEFAULT 0,
  `competitions_won` int(11) NOT NULL DEFAULT 0,
  `competitions_lost` int(11) NOT NULL DEFAULT 0,
  `competitions_drawn` int(11) NOT NULL DEFAULT 0,
  `bonus` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Heroes
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `heroes` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `char_name` varchar(35) NOT NULL DEFAULT '',
  `class_id` int(11) NOT NULL DEFAULT 0,
  `count` int(11) NOT NULL DEFAULT 0,
  `time_hero` bigint(20) NOT NULL DEFAULT 0,
  `battles` int(11) NOT NULL DEFAULT 0,
  `wins` int(11) NOT NULL DEFAULT 0,
  `losses` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `heroes_diary` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `time` bigint(20) NOT NULL DEFAULT 0,
  `action` int(11) NOT NULL DEFAULT 0,
  `param` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`char_id`,`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Cursed Weapons
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cursed_weapons` (
  `item_id` int(11) NOT NULL DEFAULT 0,
  `char_id` int(11) NOT NULL DEFAULT 0,
  `player_karma` int(11) NOT NULL DEFAULT 0,
  `player_pvp` int(11) NOT NULL DEFAULT 0,
  `time` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Pet
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pets` (
  `item_id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(25) NOT NULL DEFAULT '',
  `level` int(4) NOT NULL DEFAULT 1,
  `curHp` int(11) NOT NULL DEFAULT 1,
  `curMp` int(11) NOT NULL DEFAULT 1,
  `exp` int(11) NOT NULL DEFAULT 0,
  `sp` int(11) NOT NULL DEFAULT 0,
  `pDef` int(11) NOT NULL DEFAULT 0,
  `mDef` int(11) NOT NULL DEFAULT 0,
  `maxHp` int(11) NOT NULL DEFAULT 0,
  `maxMp` int(11) NOT NULL DEFAULT 0,
  `owner_id` int(11) NOT NULL DEFAULT 0,
  `skillid` int(11) NOT NULL DEFAULT 0,
  `skilllevel` int(3) NOT NULL DEFAULT 0,
  `summoning_points` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de BuyLists
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `buylists` (
  `buylist_id` int(11) NOT NULL DEFAULT 0,
  `item_id` int(11) NOT NULL DEFAULT 0,
  `count` int(11) NOT NULL DEFAULT 0,
  `next_recharge_time` int(11) NOT NULL DEFAULT 0,
  `current_recharge_count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`buylist_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Server Memo
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `server_memo` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `slot` int(11) NOT NULL DEFAULT 0,
  `name` varchar(255) DEFAULT NULL,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `tail` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`char_id`,`slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Bookmarks
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bookmarks` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `id` int(11) NOT NULL DEFAULT 0,
  `name` varchar(45) NOT NULL DEFAULT '',
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `env` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Offline Trade
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `offline_trade` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `char_name` varchar(35) NOT NULL DEFAULT '',
  `time` int(11) NOT NULL DEFAULT 0,
  `type` int(1) NOT NULL DEFAULT 0,
  `title` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Player Emails
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `player_emails` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `char_id` int(11) NOT NULL DEFAULT 0,
  `topic` varchar(255) NOT NULL DEFAULT '',
  `message` text,
  `unseen` int(1) NOT NULL DEFAULT 1,
  `expiration` bigint(20) NOT NULL DEFAULT 0,
  `request_id` int(11) NOT NULL DEFAULT 0,
  `system` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `char_id` (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Petition
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `petition` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `char_id` int(11) NOT NULL DEFAULT 0,
  `handler_id` int(11) NOT NULL DEFAULT -1,
  `type` int(2) NOT NULL DEFAULT 0,
  `time` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `char_id` (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `petition_message` (
  `petition_id` int(11) NOT NULL DEFAULT 0,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `text` text,
  `char_id` int(11) NOT NULL DEFAULT 0,
  `pending` int(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `petition_id` (`petition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Autofarm
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `autofarm_areas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  `radius` int(11) NOT NULL DEFAULT 0,
  `min_level` int(11) NOT NULL DEFAULT 0,
  `max_level` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `autofarm_nodes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `area_id` int(11) NOT NULL DEFAULT 0,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `area_id` (`area_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `autofarm_player_data` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `area_id` int(11) NOT NULL DEFAULT 0,
  `enabled` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`area_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `autofarm_skills` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `skill_id` int(11) NOT NULL DEFAULT 0,
  `skill_level` int(3) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `autofarm_time_usage` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `date` date NOT NULL,
  `time_used` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Balance
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `balance_classes` (
  `class_id` int(11) NOT NULL DEFAULT 0,
  `class_name` varchar(50) NOT NULL DEFAULT '',
  `melee_defense` double NOT NULL DEFAULT 1,
  `magic_defense` double NOT NULL DEFAULT 1,
  `melee_attack` double NOT NULL DEFAULT 1,
  `magic_attack` double NOT NULL DEFAULT 1,
  PRIMARY KEY (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `balance_vulnerability` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `skill_id` int(11) NOT NULL DEFAULT 0,
  `vulnerability` double NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Events
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `events_custom_data` (
  `event_name` varchar(50) NOT NULL DEFAULT '',
  `char_id` int(11) NOT NULL DEFAULT 0,
  `data` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`event_name`,`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Fishing
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fishing_championship` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `char_name` varchar(35) NOT NULL DEFAULT '',
  `fish_length` double NOT NULL DEFAULT 0,
  `fish_count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de MDT (Monsters Drop)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mdt_bets` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `bet_amount` int(11) NOT NULL DEFAULT 0,
  `bet_time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mdt_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `winner_id` int(11) NOT NULL DEFAULT 0,
  `winner_name` varchar(35) NOT NULL DEFAULT '',
  `prize` int(11) NOT NULL DEFAULT 0,
  `time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Wedding
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mods_wedding` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player1_id` int(11) NOT NULL DEFAULT 0,
  `player2_id` int(11) NOT NULL DEFAULT 0,
  `player1_name` varchar(35) NOT NULL DEFAULT '',
  `player2_name` varchar(35) NOT NULL DEFAULT '',
  `date` varchar(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `player1_id` (`player1_id`),
  KEY `player2_id` (`player2_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Auction
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bbs_auction` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `seller_id` int(11) NOT NULL DEFAULT 0,
  `seller_name` varchar(35) NOT NULL DEFAULT '',
  `item_id` int(11) NOT NULL DEFAULT 0,
  `count` int(11) NOT NULL DEFAULT 0,
  `price` int(11) NOT NULL DEFAULT 0,
  `expiration` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `seller_id` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de BBS
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bbs_favorite` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `fav_id` int(11) NOT NULL DEFAULT 0,
  `type` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`fav_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bbs_forum` (
  `forum_id` int(11) NOT NULL DEFAULT 0,
  `forum_name` varchar(255) NOT NULL DEFAULT '',
  `forum_type` int(11) NOT NULL DEFAULT 0,
  `forum_owner` int(11) NOT NULL DEFAULT 0,
  `forum_feed` int(11) NOT NULL DEFAULT 0,
  `forum_topic` int(11) NOT NULL DEFAULT 0,
  `forum_post` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`forum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bbs_mail` (
  `mail_id` int(11) NOT NULL AUTO_INCREMENT,
  `sender_id` int(11) NOT NULL DEFAULT 0,
  `receiver_id` int(11) NOT NULL DEFAULT 0,
  `read` int(1) NOT NULL DEFAULT 0,
  `expiration` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`mail_id`),
  KEY `sender_id` (`sender_id`),
  KEY `receiver_id` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bbs_post` (
  `post_id` int(11) NOT NULL AUTO_INCREMENT,
  `post_owner` int(11) NOT NULL DEFAULT 0,
  `post_owner_name` varchar(35) NOT NULL DEFAULT '',
  `post_date` bigint(20) NOT NULL DEFAULT 0,
  `topic_id` int(11) NOT NULL DEFAULT 0,
  `forum_id` int(11) NOT NULL DEFAULT 0,
  `post_text` text,
  `post_topic` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`post_id`),
  KEY `topic_id` (`topic_id`),
  KEY `forum_id` (`forum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bbs_topic` (
  `topic_id` int(11) NOT NULL AUTO_INCREMENT,
  `forum_id` int(11) NOT NULL DEFAULT 0,
  `topic_owner` int(11) NOT NULL DEFAULT 0,
  `topic_owner_name` varchar(35) NOT NULL DEFAULT '',
  `topic_name` varchar(255) NOT NULL DEFAULT '',
  `topic_date` bigint(20) NOT NULL DEFAULT 0,
  `topic_type` int(11) NOT NULL DEFAULT 0,
  `post_count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`topic_id`),
  KEY `forum_id` (`forum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Player Droplist
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `player_droplist_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `class_id` int(11) NOT NULL DEFAULT 0,
  `item_id` int(11) NOT NULL DEFAULT 0,
  `count` int(11) NOT NULL DEFAULT 0,
  `chance` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabela de Rainbowsprings
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `rainbowsprings_attacker_list` (
  `boss_id` int(11) NOT NULL DEFAULT 0,
  `attacker_id` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`boss_id`,`attacker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabela de Dungeon Cooldowns
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dungeon_cooldowns` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `dungeon_id` int(11) NOT NULL DEFAULT 0,
  `last_entry_time` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`dungeon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas de Buffshop
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `buffshop` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `buff_id` int(11) NOT NULL DEFAULT 0,
  `buff_level` int(3) NOT NULL DEFAULT 0,
  `price` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`buff_id`,`buff_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabela de Buffer Schemes
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `buffer_schemes` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `scheme_name` varchar(255) NOT NULL DEFAULT '',
  `skill_id` int(11) NOT NULL DEFAULT 0,
  `skill_level` int(3) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`,`scheme_name`,`skill_id`,`skill_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabela de Fix Player Emails Status
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fix_player_emails_status` (
  `char_id` int(11) NOT NULL DEFAULT 0,
  `status` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
