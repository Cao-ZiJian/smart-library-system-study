package com.example.library.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.dto.BookPageRequest;
import com.example.library.result.Result;
import com.example.library.service.BookCategoryService;
import com.example.library.service.BookService;
import com.example.library.vo.BookCategoryVO;
import com.example.library.vo.BookVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 图书查询接口，作为借阅业务入口。
 */
@Tag(name = "核心-图书查询")
@RestController
@RequestMapping("/book")
@Validated
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookCategoryService bookCategoryService;

    /**
     * 分页查询可借阅图书（仅上架）
     */
    @Operation(summary = "可借阅图书分页：仅返回上架图书，支撑借阅申请入口")
    @GetMapping("/page")
    public Result<Page<BookVO>> page(@Valid BookPageRequest request) {
        Page<BookVO> pageResult = bookService.pageAvailableBooks(request);
        return Result.success(pageResult);
    }

    /**
     * 图书详情
     */
    @Operation(summary = "图书详情：返回库存、分类和封面等借阅前展示信息")
    @GetMapping("/{id}")
    public Result<BookVO> detail(@PathVariable("id") Long id) {
        BookVO vo = bookService.getBookDetail(id);
        return Result.success(vo);
    }

    /**
     * 热门图书查询（按借阅次数倒序）
     */
    @Operation(summary = "热门图书：按借阅次数排序，使用 Redis 缓存")
    @GetMapping("/hot")
    public Result<List<BookVO>> hot(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<BookVO> list = bookService.listHotBooks(limit);
        return Result.success(list);
    }

    /**
     * 图书分类列表（用户端展示）
     */
    @Operation(summary = "图书分类筛选项：作为图书查询的基础条件")
    @GetMapping("/categories")
    public Result<List<BookCategoryVO>> categories() {
        List<BookCategoryVO> list = bookCategoryService.listAllCategories();
        return Result.success(list);
    }
}

































