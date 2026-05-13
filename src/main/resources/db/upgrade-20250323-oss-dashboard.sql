-- 对象存储 URL 字段、封面加长（在 smart_library 执行；已存在则跳过）

ALTER TABLE `user`
    ADD COLUMN `avatar_url` VARCHAR(1024) DEFAULT NULL COMMENT '头像图片 URL' AFTER `token_version`;

ALTER TABLE `study_room`
    ADD COLUMN `image_url` VARCHAR(1024) DEFAULT NULL COMMENT '自习室展示图 URL' AFTER `open_time`;

ALTER TABLE `book`
    MODIFY COLUMN `cover_url` VARCHAR(1024) DEFAULT NULL COMMENT '封面图片 URL';
