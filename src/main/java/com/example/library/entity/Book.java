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
 * 图书实体，对应表 book
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("book")
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 书名
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * ISBN 编号
     */
    private String isbn;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 出版年份
     */
    private Integer publishYear;

    /**
     * 封面图片地址
     */
    private String coverUrl;

    /**
     * 图书简介
     */
    private String description;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 可借库存
     */
    private Integer availableStock;

    /**
     * 借阅次数
     */
    private Integer borrowCount;

    /**
     * 状态：1 上架 0 下架
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

