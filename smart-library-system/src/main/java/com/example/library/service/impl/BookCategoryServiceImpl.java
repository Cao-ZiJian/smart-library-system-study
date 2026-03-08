package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.dto.BookCategoryAddRequest;
import com.example.library.dto.BookCategoryUpdateRequest;
import com.example.library.entity.BookCategory;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.BookCategoryMapper;
import com.example.library.service.BookCategoryService;
import com.example.library.vo.BookCategoryVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图书分类业务实现
 */
@Service
public class BookCategoryServiceImpl extends ServiceImpl<BookCategoryMapper, BookCategory> implements BookCategoryService {

    @Override
    public void addCategory(BookCategoryAddRequest request) {
        // 分类名称唯一性校验
        long count = lambdaQuery()
                .eq(BookCategory::getName, request.getName())
                .count();
        if (count > 0) {
            throw new BusinessException("分类名称已存在");
        }

        BookCategory category = new BookCategory();
        category.setName(request.getName());
        category.setSort(request.getSort());
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        if (!save(category)) {
            throw new BusinessException("新增分类失败，请稍后重试");
        }
    }

    @Override
    public void updateCategory(BookCategoryUpdateRequest request) {
        BookCategory category = getById(request.getId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        category.setName(request.getName());
        category.setSort(request.getSort());
        category.setStatus(request.getStatus());

        if (!updateById(category)) {
            throw new BusinessException("更新分类失败，请稍后重试");
        }
    }

    @Override
    public void deleteCategory(Long id) {
        if (id == null) {
            throw new BusinessException("分类ID不能为空");
        }
        boolean removed = removeById(id);
        if (!removed) {
            throw new BusinessException("分类不存在或已被删除");
        }
    }

    @Override
    public List<BookCategoryVO> listAllCategories() {
        List<BookCategory> list = lambdaQuery()
                .orderByAsc(BookCategory::getSort)
                .orderByAsc(BookCategory::getId)
                .list();

        return list.stream()
                .map(category -> {
                    BookCategoryVO vo = new BookCategoryVO();
                    BeanUtils.copyProperties(category, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }
}

