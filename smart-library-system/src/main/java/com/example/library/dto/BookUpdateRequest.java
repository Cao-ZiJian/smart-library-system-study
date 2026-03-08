package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 图书修改请求参数
 */
@Data
public class BookUpdateRequest {

    /**
     * 图书ID
     */
    @NotNull(message = "图书ID不能为空")
    private Long id;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 书名
     */
    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名长度不能超过200")
    private String title;

    /**
     * 作者
     */
    @Size(max = 100, message = "作者长度不能超过100")
    private String author;

    /**
     * ISBN 编号
     */
    @Size(max = 50, message = "ISBN 长度不能超过50")
    private String isbn;

    /**
     * 出版社
     */
    @Size(max = 100, message = "出版社长度不能超过100")
    private String publisher;

    /**
     * 出版年份
     */
    private Integer publishYear;

    /**
     * 封面图片地址
     */
    @Size(max = 500, message = "封面地址长度不能超过500")
    private String coverUrl;

    /**
     * 图书简介
     */
    @Size(max = 2000, message = "图书简介长度不能超过2000")
    private String description;

    /**
     * 总库存
     */
    @NotNull(message = "总库存不能为空")
    @Min(value = 0, message = "总库存不能为负数")
    private Integer totalStock;

    /**
     * 状态：1 上架 0 下架
     */
    private Integer status;
}

