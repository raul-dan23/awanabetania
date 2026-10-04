-- Club seasons. Exactly one is ACTIVE; the others are CLOSED and kept for reference.
-- The first season is created by the application at startup (SeasonStartup), from the
-- existing data, so this migration only adds the table.
CREATE TABLE `seasons` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(60) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `status` varchar(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seasons_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
