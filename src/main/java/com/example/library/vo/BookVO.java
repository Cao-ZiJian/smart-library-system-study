package com.example.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书对外展示对象
 */
@Data
public class BookVO {

    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

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
}

