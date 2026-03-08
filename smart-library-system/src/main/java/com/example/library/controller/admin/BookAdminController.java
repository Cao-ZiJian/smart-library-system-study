package com.example.library.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.annotation.OperationLog;
import com.example.library.dto.BookAddRequest;
import com.example.library.dto.BookPageRequest;
import com.example.library.dto.BookUpdateRequest;
import com.example.library.result.Result;
import com.example.library.service.BookService;
import com.example.library.vo.BookVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 图书管理接口（管理端）
 */
@Api(tags = "图书管理（管理端）")
@RestController
@RequestMapping("/admin/book")
@Validated
@RequiredArgsConstructor
public class BookAdminController {

    private final BookService bookService;

    /**
     * 新增图书
     */
    @OperationLog("新增图书")
    @ApiOperation("新增图书")
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody BookAddRequest request) {
        bookService.addBook(request);
        return Result.success();
    }

    /**
     * 修改图书
     */
    @OperationLog("修改图书")
    @ApiOperation("修改图书")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody BookUpdateRequest request) {
        bookService.updateBook(request);
        return Result.success();
    }

    /**
     * 分页查询图书
     */
    @ApiOperation("分页查询图书")
    @GetMapping("/page")
    public Result<Page<BookVO>> page(@Valid BookPageRequest request) {
        Page<BookVO> pageResult = bookService.pageBooks(request);
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
     * 图书上下架
     */
    @OperationLog("图书上下架")
    @ApiOperation("图书上下架")
    @PostMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable("id") Long id,
                                     @RequestParam("status") Integer status) {
        bookService.changeBookStatus(id, status);
        return Result.success();
    }
}

