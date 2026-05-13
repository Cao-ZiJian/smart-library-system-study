package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 自习室实体，对应表 study_room
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("study_room")
public class StudyRoom implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
     * 自习室展示图 URL
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

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

