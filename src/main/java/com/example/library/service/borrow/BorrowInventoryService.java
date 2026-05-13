package com.example.library.service.borrow;

import com.example.library.entity.Book;
import com.example.library.exception.BusinessException;
import com.example.library.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

import static com.example.library.config.RedisConfig.CACHE_BOOK_DETAIL;
import static com.example.library.config.RedisConfig.CACHE_BOOK_HOT;

/**
 * 借阅库存相关校验与变更
 */
@Component
@RequiredArgsConstructor
public class BorrowInventoryService {

    private final BookService bookService;

    public Book requireBookForApply(Long bookId) {
        Book book = bookService.getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        requireBookAvailable(book, "图书库存不足");
        return book;
    }

    public Book requireBookForLend(Long bookId) {
        Book book = bookService.getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        requireBookAvailable(book, "图书库存不足，无法出借");
        return book;
    }

    public Book requireExistingBook(Long bookId) {
        Book book = bookService.getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        return book;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_BOOK_DETAIL, key = "#bookId"),
            @CacheEvict(cacheNames = CACHE_BOOK_HOT, allEntries = true)
    })
    public void decreaseAvailableStock(Long bookId) {
        boolean stockOk = bookService.lambdaUpdate()
                .eq(Book::getId, bookId)
                .gt(Book::getAvailableStock, 0)
                .setSql("available_stock = available_stock - 1, borrow_count = borrow_count + 1")
                .update();
        if (!stockOk) {
            throw new BusinessException("库存扣减失败，请稍后由管理员核对库存与借阅状态");
        }
    }

    @CacheEvict(cacheNames = CACHE_BOOK_DETAIL, key = "#bookId")
    public void increaseAvailableStock(Long bookId) {
        boolean stockOk = bookService.lambdaUpdate()
                .eq(Book::getId, bookId)
                .setSql("available_stock = available_stock + 1")
                .update();
        if (!stockOk) {
            throw new BusinessException("恢复库存失败，请稍后重试");
        }
    }

    private void requireBookAvailable(Book book, String stockMessage) {
        if (book.getStatus() == null || book.getStatus() == 0) {
            throw new BusinessException("图书未上架或已下架");
        }
        if (book.getAvailableStock() == null || book.getAvailableStock() <= 0) {
            throw new BusinessException(stockMessage);
        }
    }
}
