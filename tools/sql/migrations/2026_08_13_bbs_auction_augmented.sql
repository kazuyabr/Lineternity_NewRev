-- Migration: Adicionar suporte a augmentation no auction
-- Data: 2026-08-13
-- Descricao: Adiciona colunas is_augmented e augment_id na tabela bbs_auction
--            para armazenar informacao de itens augmentados no auction house
-- Seguro para rodar multiplas vezes (IF NOT EXISTS)

ALTER TABLE bbs_auction ADD COLUMN IF NOT EXISTS `is_augmented` TINYINT(1) DEFAULT 0;
ALTER TABLE bbs_auction ADD COLUMN IF NOT EXISTS `augment_id` INT DEFAULT 0;
