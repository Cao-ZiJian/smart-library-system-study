package com.example.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.BookCategoryAddRequest;
import com.example.library.dto.BookCategoryUpdateRequest;
import com.example.library.entity.BookCategory;
import com.example.library.vo.BookCategoryVO;

import java.util.List;

/**
 * 图书分类业务接口
 */
public interface BookCategoryService extends IService<BookCategory> {

    /**
     * 新增图书分类
     *
     * @param request 请求参数
     */
    void addCategory(BookCategoryAddRequest request);

    /**
     * 修改图书分类
     *
     * @param request 请求参数
     */
    void updateCategory(BookCategoryUpdateRequest request);

    /**
     * 删除图书分类
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);

    /**
     * 查询所有图书分类
     *
     * @return 分类列表
     */
    List<BookCategoryVO> listAllCategories();
}

