CREATE TABLE IF NOT EXISTS `bbs_auction` (
	`id` INT(10) NOT NULL DEFAULT '0',
	`obj_Id` INT(10) NOT NULL DEFAULT '0',
	`item_id` INT(10) NOT NULL DEFAULT '0',
	`item_count` INT(10) NOT NULL DEFAULT '0',
	`item_enchant` INT(10) NOT NULL DEFAULT '0',
	`price_id` INT(10) NOT NULL DEFAULT '0',
	`price_count` INT(10) NOT NULL DEFAULT '0',
	`duration` BIGINT UNSIGNED DEFAULT NULL,
	`is_augmented` TINYINT(1) DEFAULT 0,
	`augment_id` INT DEFAULT 0,
	PRIMARY KEY (`id`)
);