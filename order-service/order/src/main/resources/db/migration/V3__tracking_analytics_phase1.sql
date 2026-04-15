ALTER TABLE `orders`
    ADD COLUMN `assigned_at` DATETIME(6) NULL AFTER `created_at`,
    ADD COLUMN `in_transit_at` DATETIME(6) NULL AFTER `assigned_at`,
    ADD COLUMN `order_type` VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER_ORDER' AFTER `status`;

ALTER TABLE `vehicles`
    DROP FOREIGN KEY `fk_vehicles_driver_id`;

ALTER TABLE `vehicles`
    ADD CONSTRAINT `fk_vehicles_driver_id`
        FOREIGN KEY (`driver_id`) REFERENCES `auth` (`user_id`)
        ON DELETE SET NULL ON UPDATE RESTRICT;

CREATE TABLE IF NOT EXISTS `vehicle_stats` (
    `vehicle_id` BIGINT NOT NULL,
    `total_distance_km` DOUBLE NOT NULL DEFAULT 0,
    `last_lat` DOUBLE NULL,
    `last_lng` DOUBLE NULL,
    `last_location_at` DATETIME(6) NULL,
    `active_order_count` INT NOT NULL DEFAULT 0,
    `vehicle_status` VARCHAR(30) NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_analytics` (
    `order_id` BIGINT NOT NULL,
    `order_type` VARCHAR(50) NOT NULL,
    `created_at` DATETIME(6) NULL,
    `assigned_at` DATETIME(6) NULL,
    `in_transit_at` DATETIME(6) NULL,
    `delivered_at` DATETIME(6) NULL,
    `assignment_duration_seconds` BIGINT NULL,
    `delivery_duration_seconds` BIGINT NULL,
    `vehicle_id` BIGINT NULL,
    `created_by_user_id` BIGINT NULL,
    `from_city` VARCHAR(100) NULL,
    `to_city` VARCHAR(100) NULL,
    PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `driver_stats` (
    `driver_id` BIGINT NOT NULL,
    `current_vehicle_id` BIGINT NULL,
    `delivered_order_count` BIGINT NOT NULL DEFAULT 0,
    `active_order_count` INT NOT NULL DEFAULT 0,
    `total_distance_km` DOUBLE NOT NULL DEFAULT 0,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `processed_events` (
    `event_id` VARCHAR(100) NOT NULL,
    `event_type` VARCHAR(100) NOT NULL,
    `processed_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
