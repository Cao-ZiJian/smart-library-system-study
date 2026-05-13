package com.example.library.controller.admin;

import com.example.library.annotation.RequireRole;
import com.example.library.dto.BookCategoryAddRequest;
import com.example.library.dto.BookCategoryUpdateRequest;
import com.example.library.enums.UserRole;
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
 * 图书分类基础数据维护接口。
 */
@Api(tags = "支撑-图书分类")
@RestController
@RequestMapping("/admin/book/category")
@Validated
@RequiredArgsConstructor
@RequireRole({UserRole.ADMIN, UserRole.LIBRARIAN})
public class BookCategoryAdminController {

    private final BookCategoryService bookCategoryService;

    @ApiOperation("维护图书分类：录入分类基础信息")
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody BookCategoryAddRequest request) {
        bookCategoryService.addCategory(request);
        return Result.success();
    }

    @ApiOperation("维护图书分类：调整分类名称或状态")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody BookCategoryUpdateRequest request) {
        bookCategoryService.updateCategory(request);
        return Result.success();
    }

    @ApiOperation("清理未使用图书分类：作为基础数据维护能力")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        bookCategoryService.deleteCategory(id);
        return Result.success();
    }

    @ApiOperation("图书分类列表：支撑图书筛选与后台维护")
    @GetMapping("/list")
    public Result<List<BookCategoryVO>> list() {
        List<BookCategoryVO> list = bookCategoryService.listAllCategories();
        return Result.success(list);
    }
}
