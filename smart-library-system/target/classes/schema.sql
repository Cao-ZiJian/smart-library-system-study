DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `reservation`;
DROP TABLE IF EXISTS `seat`;
DROP TABLE IF EXISTS `study_room`;
DROP TABLE IF EXISTS `borrow_order`;
DROP TABLE IF EXISTS `book`;
DROP TABLE IF EXISTS `book_category`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`     VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`     VARCHAR(200) NOT NULL COMMENT '密码（加密后）',
    `nickname`     VARCHAR(100)          DEFAULT NULL COMMENT '昵称',
    `phone`        VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `email`        VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    `role`         VARCHAR(20)  NOT NULL COMMENT '角色：USER/LIBRARIAN/ADMIN',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `book_category` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`         VARCHAR(100) NOT NULL COMMENT '分类名称',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '描述',
    `sort`         INT                   DEFAULT 0 COMMENT '排序',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

CREATE TABLE `book` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_id`    BIGINT       NOT NULL COMMENT '分类ID',
    `title`          VARCHAR(200) NOT NULL COMMENT '书名',
    `author`         VARCHAR(100)          DEFAULT NULL COMMENT '作者',
    `isbn`           VARCHAR(50)           DEFAULT NULL COMMENT 'ISBN',
    `publisher`      VARCHAR(100)          DEFAULT NULL COMMENT '出版社',
    `publish_year`   INT                   DEFAULT NULL COMMENT '出版年份',
    `cover_url`      VARCHAR(255)          DEFAULT NULL COMMENT '封面图片',
    `description`    TEXT                  DEFAULT NULL COMMENT '图书简介',
    `total_stock`    INT          NOT NULL DEFAULT 0 COMMENT '总库存',
    `available_stock` INT         NOT NULL DEFAULT 0 COMMENT '可借数量',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    `borrow_count`   INT          NOT NULL DEFAULT 0 COMMENT '借阅次数（用于热门图书）',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

CREATE TABLE `borrow_order` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT      NOT NULL COMMENT '用户ID',
    `book_id`       BIGINT      NOT NULL COMMENT '图书ID',
    `status`        VARCHAR(20) NOT NULL COMMENT '状态：APPLYING/APPROVED/LENT/RETURNED/OVERDUE/REJECTED',
    `apply_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `approve_time`  DATETIME             DEFAULT NULL COMMENT '审核时间',
    `approve_by`    BIGINT              DEFAULT NULL COMMENT '审核人（馆员）',
    `lend_time`     DATETIME             DEFAULT NULL COMMENT '出借时间',
    `due_time`      DATETIME             DEFAULT NULL COMMENT '应还时间',
    `return_time`   DATETIME             DEFAULT NULL COMMENT '归还时间',
    `return_by`     BIGINT              DEFAULT NULL COMMENT '办理归还的馆员',
    `renew_count`   INT         NOT NULL DEFAULT 0 COMMENT '续借次数',
    `remark`        VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_borrow_user` (`user_id`),
    KEY `idx_borrow_book` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅订单表';

CREATE TABLE `study_room` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`         VARCHAR(100) NOT NULL COMMENT '自习室名称',
    `location`     VARCHAR(200)          DEFAULT NULL COMMENT '位置描述',
    `capacity`     INT          NOT NULL DEFAULT 0 COMMENT '容量（座位数）',
    `open_time`    VARCHAR(50)           DEFAULT NULL COMMENT '开放时间描述，如 08:00-22:00',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室表';

CREATE TABLE `seat` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `study_room_id` BIGINT      NOT NULL COMMENT '自习室ID',
    `seat_number`  VARCHAR(50)  NOT NULL COMMENT '座位编号',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1可用 0不可用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seat_room_number` (`study_room_id`,`seat_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

CREATE TABLE `reservation` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      BIGINT      NOT NULL COMMENT '用户ID',
    `seat_id`      BIGINT      NOT NULL COMMENT '座位ID',
    `start_time`   DATETIME    NOT NULL COMMENT '预约开始时间',
    `end_time`     DATETIME    NOT NULL COMMENT '预约结束时间',
    `status`       VARCHAR(20) NOT NULL COMMENT '状态：PENDING_CHECK_IN/CANCELED/IN_USE/FINISHED/EXPIRED',
    `check_in_time` DATETIME            DEFAULT NULL COMMENT '签到时间',
    `finish_time`   DATETIME            DEFAULT NULL COMMENT '结束使用时间',
    `remark`        VARCHAR(255)        DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_reservation_user` (`user_id`),
    KEY `idx_reservation_seat` (`seat_id`),
    KEY `idx_reservation_time` (`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位预约表';

CREATE TABLE `operation_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      BIGINT                DEFAULT NULL COMMENT '操作人ID',
    `username`     VARCHAR(50)           DEFAULT NULL COMMENT '操作人用户名',
    `operation`    VARCHAR(100)          DEFAULT NULL COMMENT '操作名称',
    `method`       VARCHAR(200)          DEFAULT NULL COMMENT '方法签名',
    `request_uri`  VARCHAR(200)          DEFAULT NULL COMMENT '请求URI',
    `request_method` VARCHAR(10)         DEFAULT NULL COMMENT '请求方式',
    `request_params` TEXT                DEFAULT NULL COMMENT '请求参数',
    `ip`           VARCHAR(50)           DEFAULT NULL COMMENT 'IP地址',
    `result`       VARCHAR(20)           DEFAULT NULL COMMENT '结果：SUCCESS/FAIL',
    `error_msg`    TEXT                  DEFAULT NULL COMMENT '错误消息',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

