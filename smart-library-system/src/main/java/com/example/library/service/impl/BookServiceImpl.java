package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.dto.BookAddRequest;
import com.example.library.dto.BookPageRequest;
import com.example.library.dto.BookUpdateRequest;
import com.example.library.entity.Book;
import com.example.library.entity.BookCategory;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.BookMapper;
import com.example.library.service.BookCategoryService;
import com.example.library.service.BookService;
import com.example.library.vo.BookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.example.library.config.RedisConfig.CACHE_BOOK_HOT;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图书业务实现
 */
@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private final BookCategoryService bookCategoryService;

    @Override
    @CacheEvict(cacheNames = CACHE_BOOK_HOT, allEntries = true)
    public void addBook(BookAddRequest request) {
        // 校验分类是否存在且启用
        BookCategory category = bookCategoryService.getById(request.getCategoryId());
        if (category == null || (category.getStatus() != null && category.getStatus() == 0)) {
            throw new BusinessException("图书分类不存在或已被禁用");
        }

        Book book = new Book();
        book.setCategoryId(request.getCategoryId());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setPublishYear(request.getPublishYear());
        book.setCoverUrl(request.getCoverUrl());
        book.setDescription(request.getDescription());
        // 新增图书时，总库存与可借库存初始值一致
        book.setTotalStock(request.getTotalStock());
        book.setAvailableStock(request.getTotalStock());
        book.setBorrowCount(0);
        book.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        if (!save(book)) {
            throw new BusinessException("新增图书失败，请稍后重试");
        }
    }

    @Override
    @CacheEvict(cacheNames = CACHE_BOOK_HOT, allEntries = true)
    public void updateBook(BookUpdateRequest request) {
        Book book = getById(request.getId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }

        // 校验分类是否存在且启用
        BookCategory category = bookCategoryService.getById(request.getCategoryId());
        if (category == null || (category.getStatus() != null && category.getStatus() == 0)) {
            throw new BusinessException("图书分类不存在或已被禁用");
        }

        book.setCategoryId(request.getCategoryId());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setPublishYear(request.getPublishYear());
        book.setCoverUrl(request.getCoverUrl());
        book.setDescription(request.getDescription());
        // 更新总库存时，需要保证已借出数量不变，重新计算可借库存
        if (request.getTotalStock() != null) {
            Integer oldTotal = book.getTotalStock() == null ? 0 : book.getTotalStock();
            Integer oldAvailable = book.getAvailableStock() == null ? 0 : book.getAvailableStock();
            int borrowed = oldTotal - oldAvailable;
            if (request.getTotalStock() < borrowed) {
                throw new BusinessException("总库存不能小于已借出数量");
            }
            book.setTotalStock(request.getTotalStock());
            book.setAvailableStock(request.getTotalStock() - borrowed);
        }
        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        }

        if (!updateById(book)) {
            throw new BusinessException("修改图书失败，请稍后重试");
        }
    }

    @Override
    public Page<BookVO> pageBooks(BookPageRequest request) {
        Page<Book> page = new Page<>(request.getPageNum(), request.getPageSize());

        Page<Book> bookPage = lambdaQuery()
                .eq(request.getCategoryId() != null, Book::getCategoryId, request.getCategoryId())
                .like(StringUtils.hasText(request.getKeyword()), Book::getTitle, request.getKeyword())
                .eq(request.getStatus() != null, Book::getStatus, request.getStatus())
                .orderByDesc(Book::getCreateTime)
                .page(page);

        return buildBookVOPage(bookPage);
    }

    @Override
    public Page<BookVO> pageAvailableBooks(BookPageRequest request) {
        Page<Book> page = new Page<>(request.getPageNum(), request.getPageSize());

        Page<Book> bookPage = lambdaQuery()
                .eq(Book::getStatus, 1)
                .eq(request.getCategoryId() != null, Book::getCategoryId, request.getCategoryId())
                .like(StringUtils.hasText(request.getKeyword()), Book::getTitle, request.getKeyword())
                .orderByDesc(Book::getCreateTime)
                .page(page);

        return buildBookVOPage(bookPage);
    }

    @Override
    public BookVO getBookDetail(Long id) {
        Book book = getById(id);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }

        BookCategory category = bookCategoryService.getById(book.getCategoryId());
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        return vo;
    }

    @Override
    @CacheEvict(cacheNames = CACHE_BOOK_HOT, allEntries = true)
    public void changeBookStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException("图书ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("图书状态不合法");
        }

        Book book = getById(id);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }

        book.setStatus(status);
        if (!updateById(book)) {
            throw new BusinessException("更新图书状态失败，请稍后重试");
        }
    }

    @Override
    @Cacheable(cacheNames = CACHE_BOOK_HOT, key = "#limit")
    public List<BookVO> listHotBooks(int limit) {
        if (limit <= 0 || limit > 20) {
            limit = 10;
        }

        List<Book> books = lambdaQuery()
                .eq(Book::getStatus, 1)
                .orderByDesc(Book::getBorrowCount)
                .last("limit " + limit)
                .list();

        if (books.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, BookCategory> categoryMap = buildCategoryMap(books);

        return books.stream()
                .map(book -> toVO(book, categoryMap.get(book.getCategoryId())))
                .collect(Collectors.toList());
    }

    private Page<BookVO> buildBookVOPage(Page<Book> bookPage) {
        List<Book> records = bookPage.getRecords();
        if (records == null || records.isEmpty()) {
            Page<BookVO> emptyPage = new Page<>(bookPage.getCurrent(), bookPage.getSize(), bookPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        Map<Long, BookCategory> categoryMap = buildCategoryMap(records);

        List<BookVO> voRecords = records.stream()
                .map(book -> toVO(book, categoryMap.get(book.getCategoryId())))
                .collect(Collectors.toList());

        Page<BookVO> voPage = new Page<>(bookPage.getCurrent(), bookPage.getSize(), bookPage.getTotal());
        voPage.setRecords(voRecords);
        return voPage;
    }

    private Map<Long, BookCategory> buildCategoryMap(List<Book> books) {
        Set<Long> categoryIds = books.stream()
                .map(Book::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<BookCategory> categories = bookCategoryService.listByIds(categoryIds);
        return categories.stream()
                .collect(Collectors.toMap(BookCategory::getId, c -> c));
    }

    private BookVO toVO(Book book, BookCategory category) {
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        return vo;
    }
}

