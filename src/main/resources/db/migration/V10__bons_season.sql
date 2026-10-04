-- The season each row belongs to. Nullable so the previous version, which does not know
-- about seasons, keeps working during the deploy pre-flight and after a rollback; rows it
-- writes are given the active season at the next start (SeasonStartup).
ALTER TABLE `bons`
  ADD COLUMN `season_id` int DEFAULT NULL,
  ADD KEY `idx_bons_season` (`season_id`),
  ADD CONSTRAINT `fk_bons_season` FOREIGN KEY (`season_id`) REFERENCES `seasons` (`id`);
