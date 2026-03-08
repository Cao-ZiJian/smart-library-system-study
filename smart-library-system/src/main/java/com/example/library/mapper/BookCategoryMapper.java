package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.BookCategory;
import org.springframework.stereotype.Repository;

/**
 * 图书分类 Mapper
 */
@Repository
public interface BookCategoryMapper extends BaseMapper<BookCategory> {
}

