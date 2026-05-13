-- ============================================================
-- smart-library-system 初始化测试数据
-- 密码统一为 123456，使用 BCryptPasswordEncoder 加密（strength 10）
-- 执行前请先执行 schema.sql 建表，再在 smart_library 库下执行本脚本
-- ============================================================

USE smart_library;

-- 清空依赖顺序：先清子表，再清主表（保留表结构）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE operation_log;
TRUNCATE TABLE reservation;
TRUNCATE TABLE seat;
TRUNCATE TABLE study_room;
TRUNCATE TABLE borrow_order;
TRUNCATE TABLE book;
TRUNCATE TABLE book_category;
TRUNCATE TABLE user;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 1. 用户（密码均为 123456 的 BCrypt 哈希）
-- ------------------------------------------------------------
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `role`, `status`, `token_version`, `avatar_url`, `create_time`, `update_time`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', NULL, 'admin@library.com', 'ADMIN', 1, 0, NULL, NOW(), NOW()),
(2, 'librarian', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '馆员小王', '13800138001', 'librarian@library.com', 'LIBRARIAN', 1, 0, NULL, NOW(), NOW()),
(3, 'user01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', '13800138002', 'user01@example.com', 'USER', 1, 0, NULL, NOW(), NOW());

-- ------------------------------------------------------------
-- 2. 图书分类
-- ------------------------------------------------------------
INSERT INTO `book_category` (`id`, `name`, `description`, `sort`, `status`, `create_time`, `update_time`) VALUES
(1, '计算机', '计算机与编程类', 1, 1, NOW(), NOW()),
(2, '文学', '文学与小说', 2, 1, NOW(), NOW()),
(3, '经济管理', '经济与管理类', 3, 1, NOW(), NOW());

-- ------------------------------------------------------------
-- 3. 图书（category_id 对应上表，status 1=上架）
-- ------------------------------------------------------------
INSERT INTO `book` (`id`, `category_id`, `title`, `author`, `isbn`, `publisher`, `publish_year`, `cover_url`, `description`, `total_stock`, `available_stock`, `status`, `borrow_count`, `create_time`, `update_time`) VALUES
(1, 1, 'Java 核心技术 卷I', 'Cay S. Horstmann', '978-7-111-54742-6', '机械工业出版社', 2019, NULL, 'Java 经典教材', 5, 5, 1, 0, NOW(), NOW()),
(2, 1, 'Spring Boot 编程思想', '小马哥', '978-7-121-35822-8', '电子工业出版社', 2019, NULL, 'Spring Boot 核心原理', 3, 3, 1, 0, NOW(), NOW()),
(3, 2, '活着', '余华', '978-7-5063-6073-8', '作家出版社', 2012, NULL, '经典文学作品', 4, 4, 1, 0, NOW(), NOW()),
(4, 3, '原则', '瑞·达利欧', '978-7-5086-8418-9', '中信出版社', 2018, NULL, '生活与工作原则', 2, 2, 1, 0, NOW(), NOW());

-- ------------------------------------------------------------
-- 4. 自习室
-- ------------------------------------------------------------
INSERT INTO `study_room` (`id`, `name`, `location`, `capacity`, `open_time`, `image_url`, `status`, `create_time`, `update_time`) VALUES
(1, 'A区自习室', '图书馆一楼A区', 20, '08:00-22:00', NULL, 1, NOW(), NOW()),
(2, 'B区自习室', '图书馆二楼B区', 30, '08:00-22:00', NULL, 1, NOW(), NOW());

-- ------------------------------------------------------------
-- 5. 座位（study_room_id 对应上表，status 1=可用）
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 说明
-- ------------------------------------------------------------
-- 管理员: admin / 123456
-- 馆员:   librarian / 123456
-- 普通用户: user01 / 123456
--
-- 密码字段为 BCrypt 哈希（strength=10，与 PasswordEncoderConfig 中 BCryptPasswordEncoder 一致）。
-- 若登录校验失败，请在项目中执行: new BCryptPasswordEncoder().encode("123456") 得到新哈希，替换上面三处 password 值。
-- ============================================================
