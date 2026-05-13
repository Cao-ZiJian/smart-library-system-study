-- ============================================================
-- Smart library engineering upgrade script.
-- Run this script against the smart_library database after backup.
-- Skip statements manually if the same column or index already exists.
-- ============================================================

-- 1. User session version, used with JWT + Redis Session invalidation.
ALTER TABLE `user`
    ADD COLUMN `token_version` INT NOT NULL DEFAULT 0 COMMENT 'Session version. Old login sessions become invalid after increment.' AFTER `status`;

-- 2. Book category name uniqueness, aligned with application validation.
ALTER TABLE `book_category`
    ADD UNIQUE KEY `uk_book_category_name` (`name`);

-- 3. Common borrow order composite indexes.
ALTER TABLE `borrow_order`
    ADD KEY `idx_borrow_user_status` (`user_id`, `status`),
    ADD KEY `idx_borrow_status_due` (`status`, `due_time`);

-- 4. Reservation conflict scan index by seat, status, and time range.
ALTER TABLE `reservation`
    ADD KEY `idx_reservation_seat_status_time` (`seat_id`, `status`, `start_time`, `end_time`);

-- Note: user.username and seat(study_room_id, seat_number) are already unique in the initial schema.
