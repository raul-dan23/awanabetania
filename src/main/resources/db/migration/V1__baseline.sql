-- Baseline: the schema as Hibernate (ddl-auto=update) created it before Flyway took over.
--
-- Production already has these tables, so Flyway does NOT run this file there: on the first
-- start it records the existing schema as version 1 (spring.flyway.baseline-on-migrate) and
-- applies only V2 and later. On an empty database (a new install, CI, local development)
-- this file creates the full schema.
--
-- Taken with SHOW CREATE TABLE from a MySQL 8.0 database built by ddl-auto=update from the
-- current entities, so keys and index names are the ones production got from Hibernate and
-- later migrations can refer to them by name. Tables are ordered so that every foreign key
-- points to a table created before it.
--
-- Never edit this file: Flyway checksums applied migrations. Change the schema with a new
-- V<n>__description.sql file instead.

CREATE TABLE `children` (
  `id` int NOT NULL AUTO_INCREMENT,
  `attendance_streak` int DEFAULT NULL,
  `badges_count` int DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `current_team` varchar(255) DEFAULT NULL,
  `daily_points` int DEFAULT NULL,
  `deletion_code` varchar(255) DEFAULT NULL,
  `has_hat` bit(1) DEFAULT NULL,
  `has_manual` bit(1) DEFAULT NULL,
  `has_shirt` bit(1) DEFAULT NULL,
  `is_suspended` bit(1) DEFAULT NULL,
  `last_attendance_date` date DEFAULT NULL,
  `lessons_completed` int DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `nfc_uid` varchar(255) DEFAULT NULL,
  `parent_name` varchar(255) DEFAULT NULL,
  `parent_phone` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `progress_percent` int DEFAULT NULL,
  `season_points` int DEFAULT NULL,
  `surname` varchar(255) DEFAULT NULL,
  `total_attendance` int DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkpq6pade7a5dxj8v1rkbrcudr` (`nfc_uid`),
  UNIQUE KEY `UK7048w76wa956yje033iw27kji` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `leaders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `deletion_code` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `notes` text,
  `password` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `rating` float DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `surname` varchar(255) NOT NULL,
  `username` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKaa9fak0rnb94h9g9lhqxqxrjy` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `departments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `max_leaders` int DEFAULT NULL,
  `min_leaders` int DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `head_leader_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKq9efgupvut9cxcxpmc8vb8u20` (`head_leader_id`),
  CONSTRAINT `FKe0d6kp05bg8ccuebsg1j0ns` FOREIGN KEY (`head_leader_id`) REFERENCES `leaders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `leaders_departments` (
  `leader_id` int NOT NULL,
  `department_id` int NOT NULL,
  PRIMARY KEY (`leader_id`,`department_id`),
  KEY `FKb2tybne6khgsgdvq3yg0w7mxb` (`department_id`),
  CONSTRAINT `FK2lw4da532bpmk9wid1p62cesj` FOREIGN KEY (`leader_id`) REFERENCES `leaders` (`id`),
  CONSTRAINT `FKb2tybne6khgsgdvq3yg0w7mxb` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `meetings` (
  `id` int NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `general_feedback` text,
  `general_rating` int DEFAULT NULL,
  `is_completed` bit(1) DEFAULT NULL,
  `meeting_pin` varchar(255) DEFAULT NULL,
  `director_day_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnv997ows1odqwc3g88i7xugnx` (`director_day_id`),
  CONSTRAINT `FKnv997ows1odqwc3g88i7xugnx` FOREIGN KEY (`director_day_id`) REFERENCES `leaders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `meeting_assignments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `status` varchar(255) DEFAULT NULL,
  `department_id` int DEFAULT NULL,
  `leader_id` int DEFAULT NULL,
  `meeting_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmmgnd8f8jscltjq90rwrfkc9h` (`department_id`),
  KEY `FKrv6xp54m4o4ytge61yqw4n2vq` (`leader_id`),
  KEY `FKm06iowa1x6nke33pmrdl7slxu` (`meeting_id`),
  CONSTRAINT `FKm06iowa1x6nke33pmrdl7slxu` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`),
  CONSTRAINT `FKmmgnd8f8jscltjq90rwrfkc9h` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
  CONSTRAINT `FKrv6xp54m4o4ytge61yqw4n2vq` FOREIGN KEY (`leader_id`) REFERENCES `leaders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `leader_evaluations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `comment` text,
  `date` date DEFAULT NULL,
  `evaluated_by` int DEFAULT NULL,
  `is_visible` bit(1) DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `leader_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKc1f9qmcphc8ymc1f87svbgpwf` (`leader_id`),
  CONSTRAINT `FKc1f9qmcphc8ymc1f87svbgpwf` FOREIGN KEY (`leader_id`) REFERENCES `leaders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `child_manual` (
  `id` int NOT NULL AUTO_INCREMENT,
  `end_date` date DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `child_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlubk5xichufo8b0nmicckdgbq` (`child_id`),
  CONSTRAINT `FKlubk5xichufo8b0nmicckdgbq` FOREIGN KEY (`child_id`) REFERENCES `children` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `child_progress` (
  `id` int NOT NULL AUTO_INCREMENT,
  `last_sticker_id` int DEFAULT NULL,
  `manuals_count` int DEFAULT NULL,
  `child_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKp80p0b0579iarlf1w7gqqxl8o` (`child_id`),
  CONSTRAINT `FK1vfpg7bvg7i5bj84yypwt623g` FOREIGN KEY (`child_id`) REFERENCES `children` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bons` (
  `id` int NOT NULL AUTO_INCREMENT,
  `approved_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `items` text,
  `leader_name` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `total_points` int DEFAULT NULL,
  `child_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3o1egi3tg24dupbq2tvvp8wvm` (`child_id`),
  CONSTRAINT `FK3o1egi3tg24dupbq2tvvp8wvm` FOREIGN KEY (`child_id`) REFERENCES `children` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `scores` (
  `id` int NOT NULL AUTO_INCREMENT,
  `attended` bit(1) DEFAULT NULL,
  `date` date NOT NULL,
  `details` text,
  `extra_points` int DEFAULT NULL,
  `friend` bit(1) DEFAULT NULL,
  `has_bible` bit(1) DEFAULT NULL,
  `has_handbook` bit(1) DEFAULT NULL,
  `has_uniform` bit(1) DEFAULT NULL,
  `individual_points` int DEFAULT NULL,
  `lesson` bit(1) DEFAULT NULL,
  `team_points` int DEFAULT NULL,
  `total` int DEFAULT NULL,
  `child_id` int NOT NULL,
  `meeting_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj72qk0v5cvfmxm1fhm7fdci4l` (`child_id`),
  KEY `FKcppay3qprq2sd9fkg7ht19bln` (`meeting_id`),
  CONSTRAINT `FKcppay3qprq2sd9fkg7ht19bln` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`),
  CONSTRAINT `FKj72qk0v5cvfmxm1fhm7fdci4l` FOREIGN KEY (`child_id`) REFERENCES `children` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `warnings` (
  `id` int NOT NULL AUTO_INCREMENT,
  `date` date DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `remaining_meetings` int DEFAULT NULL,
  `suspension` bit(1) DEFAULT NULL,
  `child_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKklqwflhusj0lo8diiqaktcqnp` (`child_id`),
  CONSTRAINT `FKklqwflhusj0lo8diiqaktcqnp` FOREIGN KEY (`child_id`) REFERENCES `children` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `team_game_points` (
  `id` int NOT NULL AUTO_INCREMENT,
  `points` int DEFAULT NULL,
  `team_name` varchar(255) DEFAULT NULL,
  `meeting_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK94gnpq9vghnjhl6f3pfwmuwxg` (`meeting_id`),
  CONSTRAINT `FK94gnpq9vghnjhl6f3pfwmuwxg` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notifications` (
  `id` int NOT NULL AUTO_INCREMENT,
  `child_id` int DEFAULT NULL,
  `date` date DEFAULT NULL,
  `is_visible` bit(1) DEFAULT NULL,
  `message` text,
  `type` varchar(255) DEFAULT NULL,
  `visible_to` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `olimpiada_sessions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(10) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7tpqvum42c0p50egebeim7lhb` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `olimpiada_scores` (
  `id` int NOT NULL AUTO_INCREMENT,
  `arbiter_name` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_double` bit(1) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  `place` int DEFAULT NULL,
  `points` int DEFAULT NULL,
  `round_number` int DEFAULT NULL,
  `session_id` int DEFAULT NULL,
  `team` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `products` (
  `id` int NOT NULL AUTO_INCREMENT,
  `is_available` bit(1) DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `point_price` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sticker` (
  `id` int NOT NULL AUTO_INCREMENT,
  `image_path` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
