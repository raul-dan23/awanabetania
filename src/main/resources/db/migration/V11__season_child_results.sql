-- What each child had when a season closed, before the new season set it back to zero.
CREATE TABLE `season_child_results` (
  `id` int NOT NULL AUTO_INCREMENT,
  `season_id` int NOT NULL,
  `child_id` int NOT NULL,
  `season_points` int NOT NULL,
  `total_attendance` int NOT NULL,
  `attendance_streak` int NOT NULL,
  `lessons_completed` int NOT NULL,
  `badges_count` int NOT NULL,
  `had_manual` bit(1) NOT NULL,
  `had_shirt` bit(1) NOT NULL,
  `had_hat` bit(1) NOT NULL,
  `manuals` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_season_child_results` (`season_id`,`child_id`),
  KEY `idx_season_child_results_child` (`child_id`),
  CONSTRAINT `fk_season_child_results_season` FOREIGN KEY (`season_id`) REFERENCES `seasons` (`id`),
  CONSTRAINT `fk_season_child_results_child` FOREIGN KEY (`child_id`) REFERENCES `children` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
