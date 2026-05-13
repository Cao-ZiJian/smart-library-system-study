package com.example.library.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.annotation.OperationLog;
import com.example.library.annotation.RequireRole;
import com.example.library.dto.BookAddRequest;
import com.example.library.dto.BookPageRequest;
import com.example.library.dto.BookUpdateRequest;
import com.example.library.enums.UserRole;
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
 * 图书基础数据维护接口，作为借阅业务支撑。
 */
@Api(tags = "支撑-图书基础数据")
@RestController
@RequestMapping("/admin/book")
@Validated
@RequiredArgsConstructor
@RequireRole({UserRole.ADMIN, UserRole.LIBRARIAN})
public class BookAdminController {

    private final BookService bookService;

    @OperationLog("新增图书")
    @ApiOperation("录入图书基础信息：用于借阅资源准备")
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody BookAddRequest request) {
        bookService.addBook(request);
        return Result.success();
    }

    @OperationLog("修改图书")
    @ApiOperation("维护图书基础信息：分类、库存、封面和描述")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody BookUpdateRequest request) {
        bookService.updateBook(request);
        return Result.success();
    }

    @ApiOperation("馆藏图书分页：用于后台核对借阅资源")
    @GetMapping("/page")
    public Result<Page<BookVO>> page(@Valid BookPageRequest request) {
        Page<BookVO> pageResult = bookService.pageBooks(request);
        return Result.success(pageResult);
    }

    @ApiOperation("馆藏图书详情：查看库存与展示信息")
    @GetMapping("/{id}")
    public Result<BookVO> detail(@PathVariable("id") Long id) {
        BookVO vo = bookService.getBookDetail(id);
        return Result.success(vo);
    }

    @OperationLog("图书上下架")
    @ApiOperation("调整图书借阅可用状态：控制是否出现在用户端查询")
    @PostMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable("id") Long id,
                                     @RequestParam("status") Integer status) {
        bookService.changeBookStatus(id, status);
        return Result.success();
    }
}
