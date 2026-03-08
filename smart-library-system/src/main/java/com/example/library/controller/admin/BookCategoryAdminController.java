package com.example.library.controller.admin;

import com.example.library.dto.BookCategoryAddRequest;
import com.example.library.dto.BookCategoryUpdateRequest;
import com.example.library.result.Result;
import com.example.library.service.BookCategoryService;
import com.example.library.vo.BookCategoryVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 图书分类管理接口（管理端）
 */
@Api(tags = "图书分类管理（管理端）")
@RestController
@RequestMapping("/admin/book/category")
@Validated
@RequiredArgsConstructor
public class BookCategoryAdminController {

    private final BookCategoryService bookCategoryService;

    /**
     * 新增图书分类
     */
    @ApiOperation("新增图书分类")
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody BookCategoryAddRequest request) {
        bookCategoryService.addCategory(request);
        return Result.success();
    }

    /**
     * 修改图书分类
     */
    @ApiOperation("修改图书分类")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody BookCategoryUpdateRequest request) {
        bookCategoryService.updateCategory(request);
        return Result.success();
    }

    /**
     * 删除图书分类
     */
    @ApiOperation("删除图书分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        bookCategoryService.deleteCategory(id);
        return Result.success();
    }

    /**
     * 图书分类列表
     */
    @ApiOperation("图书分类列表")
    @GetMapping("/list")
    public Result<List<BookCategoryVO>> list() {
        List<BookCategoryVO> list = bookCategoryService.listAllCategories();
        return Result.success(list);
    }
}

