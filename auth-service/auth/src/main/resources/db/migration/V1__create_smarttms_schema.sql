CREATE TABLE `auth` (
    `user_id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_auth_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user` (
    `user_profile_id` BIGINT NOT NULL AUTO_INCREMENT,
    `auth_user_id` BIGINT NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `first_name` VARCHAR(100) NOT NULL,
    `last_name` VARCHAR(100) NULL,
    `user_role` VARCHAR(30) NOT NULL,
    `phone_number` VARCHAR(30) NULL,
    `city` VARCHAR(100) NULL,
    PRIMARY KEY (`user_profile_id`),
    UNIQUE KEY `uk_user_auth_user_id` (`auth_user_id`),
    CONSTRAINT `fk_user_auth_user_id`
        FOREIGN KEY (`auth_user_id`) REFERENCES `auth` (`user_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `vehicles` (
    `vehicle_id` BIGINT NOT NULL AUTO_INCREMENT,
    `driver_id` BIGINT NULL,
    `vehicle_name` VARCHAR(150) NOT NULL,
    `vehicle_code` VARCHAR(100) NOT NULL,
    `vehicle_type` VARCHAR(30) NOT NULL,
    `from` VARCHAR(100) NOT NULL,
    `to` VARCHAR(100) NOT NULL,
    `vehicle_status` VARCHAR(30) NULL,
    PRIMARY KEY (`vehicle_id`),
    UNIQUE KEY `uk_vehicles_vehicle_code` (`vehicle_code`),
    KEY `idx_vehicles_driver_id` (`driver_id`),
    CONSTRAINT `fk_vehicles_driver_id`
        FOREIGN KEY (`driver_id`) REFERENCES `user` (`user_profile_id`)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `orders` (
    `order_id` BIGINT NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6) NOT NULL,
    `delivered_at` DATETIME(6) NULL,
    `from_city` VARCHAR(100) NOT NULL,
    `to_city` VARCHAR(100) NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    `vehicle_id` BIGINT NULL,
    PRIMARY KEY (`order_id`),
    KEY `idx_orders_vehicle_id` (`vehicle_id`),
    CONSTRAINT `fk_orders_vehicle_id`
        FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`vehicle_id`)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `vehicle_through_points` (
    `vehicle_id` BIGINT NOT NULL,
    `through_point` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`vehicle_id`, `through_point`),
    CONSTRAINT `fk_vehicle_through_points_vehicle_id`
        FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`vehicle_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `vehicle_orders` (
    `vehicle_id` BIGINT NOT NULL,
    `order_id` BIGINT NOT NULL,
    PRIMARY KEY (`vehicle_id`, `order_id`),
    UNIQUE KEY `uk_vehicle_orders_order_id` (`order_id`),
    CONSTRAINT `fk_vehicle_orders_vehicle_id`
        FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`vehicle_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_vehicle_orders_order_id`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
