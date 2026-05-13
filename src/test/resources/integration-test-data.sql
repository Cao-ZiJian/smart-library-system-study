-- 集成测试用种子数据（无 USE/TRUNCATE，配合 classpath:schema.sql 使用）
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `role`, `status`, `token_version`, `avatar_url`, `create_time`, `update_time`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', NULL, 'admin@library.com', 'ADMIN', 1, 0, NULL, NOW(), NOW()),
(2, 'librarian', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '馆员小王', '13800138001', 'librarian@library.com', 'LIBRARIAN', 1, 0, NULL, NOW(), NOW()),
(3, 'user01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', '13800138002', 'user01@example.com', 'USER', 1, 0, NULL, NOW(), NOW());

INSERT INTO `book_category` (`id`, `name`, `description`, `sort`, `status`, `create_time`, `update_time`) VALUES
(1, '计算机', '计算机与编程类', 1, 1, NOW(), NOW()),
(2, '文学', '文学与小说', 2, 1, NOW(), NOW()),
(3, '经济管理', '经济与管理类', 3, 1, NOW(), NOW());

INSERT INTO `book` (`id`, `category_id`, `title`, `author`, `isbn`, `publisher`, `publish_year`, `cover_url`, `description`, `total_stock`, `available_stock`, `status`, `borrow_count`, `create_time`, `update_time`) VALUES
(1, 1, 'Java 核心技术 卷I', 'Cay S. Horstmann', '978-7-111-54742-6', '机械工业出版社', 2019, NULL, 'Java 经典教材', 5, 5, 1, 0, NOW(), NOW()),
(2, 1, 'Spring Boot 编程思想', '小马哥', '978-7-121-35822-8', '电子工业出版社', 2019, NULL, 'Spring Boot 核心原理', 3, 3, 1, 0, NOW(), NOW()),
(3, 2, '活着', '余华', '978-7-5063-6073-8', '作家出版社', 2012, NULL, '经典文学作品', 4, 4, 1, 0, NOW(), NOW()),
(4, 3, '原则', '瑞·达利欧', '978-7-5086-8418-9', '中信出版社', 2018, NULL, '生活与工作原则', 2, 2, 1, 0, NOW(), NOW());

INSERT INTO `study_room` (`id`, `name`, `location`, `capacity`, `open_time`, `image_url`, `status`, `create_time`, `update_time`) VALUES
(1, 'A区自习室', '图书馆一楼A区', 20, '08:00-22:00', NULL, 1, NOW(), NOW()),
(2, 'B区自习室', '图书馆二楼B区', 30, '08:00-22:00', NULL, 1, NOW(), NOW());

INSERT INTO `seat` (`id`, `study_room_id`, `seat_number`, `status`, `create_time`, `update_time`) VALUES
(1, 1, 'A-01', 1, NOW(), NOW()),
(2, 1, 'A-02', 1, NOW(), NOW()),
(3, 1, 'A-03', 1, NOW(), NOW()),
(4, 1, 'A-04', 1, NOW(), NOW()),
(5, 1, 'A-05', 1, NOW(), NOW()),
(6, 2, 'B-01', 1, NOW(), NOW()),
(7, 2, 'B-02', 1, NOW(), NOW()),
(8, 2, 'B-03', 1, NOW(), NOW()),
(9, 2, 'B-04', 1, NOW(), NOW()),
(10, 2, 'B-05', 1, NOW(), NOW());
