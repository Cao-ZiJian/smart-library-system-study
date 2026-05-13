package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.Book;
import org.springframework.stereotype.Repository;

/**
 * 图书 Mapper
 */
@Repository
public interface BookMapper extends BaseMapper<Book> {
}

