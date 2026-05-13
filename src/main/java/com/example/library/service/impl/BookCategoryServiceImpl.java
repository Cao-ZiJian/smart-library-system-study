package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.dto.BookCategoryAddRequest;
import com.example.library.dto.BookCategoryUpdateRequest;
import com.example.library.entity.Book;
import com.example.library.entity.BookCategory;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.BookCategoryMapper;
import com.example.library.mapper.BookMapper;
import com.example.library.service.BookCategoryService;
import com.example.library.vo.BookCategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import static com.example.library.config.RedisConfig.CACHE_BOOK_CATEGORY_LIST;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图书分类业务实现
 */
@Service
@RequiredArgsConstructor
public class BookCategoryServiceImpl extends ServiceImpl<BookCategoryMapper, BookCategory> implements BookCategoryService {

    private final BookMapper bookMapper;

    @Override
    @CacheEvict(cacheNames = CACHE_BOOK_CATEGORY_LIST, allEntries = true)
    public void addCategory(BookCategoryAddRequest request) {
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

        try {
            if (!save(category)) {
                throw new BusinessException("新增分类失败，请稍后重试");
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("分类名称已存在");
        }
    }

    @Override
    @CacheEvict(cacheNames = CACHE_BOOK_CATEGORY_LIST, allEntries = true)
    public void updateCategory(BookCategoryUpdateRequest request) {
        BookCategory category = getById(request.getId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        long dup = lambdaQuery()
                .eq(BookCategory::getName, request.getName())
                .ne(BookCategory::getId, request.getId())
                .count();
        if (dup > 0) {
            throw new BusinessException("分类名称已存在");
        }

        category.setName(request.getName());
        category.setSort(request.getSort());
        category.setStatus(request.getStatus());

        try {
            if (!updateById(category)) {
                throw new BusinessException("更新分类失败，请稍后重试");
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("分类名称已存在");
        }
    }

    @Override
    @CacheEvict(cacheNames = CACHE_BOOK_CATEGORY_LIST, allEntries = true)
    public void deleteCategory(Long id) {
        if (id == null) {
            throw new BusinessException("分类ID不能为空");
        }
        long ref = bookMapper.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getCategoryId, id));
        if (ref > 0) {
            throw new BusinessException("该分类下仍有图书，无法删除");
        }
        boolean removed = removeById(id);
        if (!removed) {
            throw new BusinessException("分类不存在或已被删除");
        }
    }

    @Override
    @Cacheable(cacheNames = CACHE_BOOK_CATEGORY_LIST, key = "'all'")
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
