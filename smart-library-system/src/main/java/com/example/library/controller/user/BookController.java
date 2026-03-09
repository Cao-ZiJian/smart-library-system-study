package com.example.library.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.dto.BookPageRequest;
import com.example.library.result.Result;
import com.example.library.service.BookCategoryService;
import com.example.library.service.BookService;
import com.example.library.vo.BookCategoryVO;
import com.example.library.vo.BookVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 图书接口（用户端）
 */
@Api(tags = "图书模块（用户端）")
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
    @ApiOperation("分页查询可借阅图书")
    @GetMapping("/page")
    public Result<Page<BookVO>> page(@Valid BookPageRequest request) {
        Page<BookVO> pageResult = bookService.pageAvailableBooks(request);
        return Result.success(pageResult);
    }

    /**
     * 图书详情
     */
    @ApiOperation("图书详情")
    @GetMapping("/{id}")
    public Result<BookVO> detail(@PathVariable("id") Long id) {
        BookVO vo = bookService.getBookDetail(id);
        return Result.success(vo);
    }

    /**
     * 热门图书查询（按借阅次数倒序）
     */
    @ApiOperation("热门图书查询")
    @GetMapping("/hot")
    public Result<List<BookVO>> hot(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<BookVO> list = bookService.listHotBooks(limit);
        return Result.success(list);
    }

    /**
     * 图书分类列表（用户端展示）
     */
    @ApiOperation("图书分类列表")
    @GetMapping("/categories")
    public Result<List<BookCategoryVO>> categories() {
        List<BookCategoryVO> list = bookCategoryService.listAllCategories();
        return Result.success(list);
    }
}

































