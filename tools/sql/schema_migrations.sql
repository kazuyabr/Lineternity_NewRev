-- Tabela de controle de migracoes do schema
-- Usado pelo stack.py e entrypoint.sh para saber quais migracoes ja foram aplicadas

CREATE TABLE IF NOT EXISTS `schema_migrations` (
	`id` INT(10) NOT NULL AUTO_INCREMENT,
	`filename` VARCHAR(255) NOT NULL,
	`applied_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	UNIQUE KEY `uk_filename` (`filename`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
