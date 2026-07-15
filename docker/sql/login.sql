-- ============================================================
-- Tabelas de Login (LoginServer Database)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- Tabela de contas
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `accounts` (
  `login` VARCHAR(45) NOT NULL DEFAULT '',
  `password` VARCHAR(60) NOT NULL DEFAULT '',
  `last_active` BIGINT NOT NULL DEFAULT 0,
  `access_level` INT(3) NOT NULL DEFAULT 0,
  `last_server` INT(4) NOT NULL DEFAULT 1,
  PRIMARY KEY (`login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabela de gameservers registrados
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `gameservers` (
  `server_id` int(11) NOT NULL DEFAULT 0,
  `hexid` varchar(50) NOT NULL DEFAULT '',
  `host` varchar(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Tabelas HWID (banimento e controle)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `hwid_bans` (
  `HWID` varchar(32) DEFAULT NULL,
  `HWIDSecond` varchar(32) DEFAULT NULL,
  `expiretime` int(11) NOT NULL DEFAULT 0,
  `comments` varchar(255) DEFAULT '',
  UNIQUE KEY `HWID` (`HWID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `hwid_extra_boxes` (
  `hwid` varchar(64) NOT NULL,
  `extra_boxes` int(11) NOT NULL,
  PRIMARY KEY (`hwid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hwid_info` (
  `HWID` varchar(32) NOT NULL DEFAULT '',
  `Account` varchar(45) NOT NULL DEFAULT '',
  `PlayerID` int(10) unsigned NOT NULL DEFAULT 0,
  `LockType` enum('PLAYER_LOCK','ACCOUNT_LOCK','NONE') NOT NULL DEFAULT 'NONE',
  PRIMARY KEY (`HWID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

SET FOREIGN_KEY_CHECKS = 1;
