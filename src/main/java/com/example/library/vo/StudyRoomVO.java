package com.example.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自习室对外展示对象
 */
@Data
public class StudyRoomVO {

    private Long id;

    /**
     * 自习室名称
     */
    private String name;

    /**
     * 位置描述
     */
    private String location;

    /**
     * 容量（座位数）
     */
    private Integer capacity;

    /**
     * 开放时间描述，如 08:00-22:00
     */
    private String openTime;

    /**
     * 展示图 URL
     */
    private String imageUrl;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

