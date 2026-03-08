package com.example.library.controller.librarian;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.dto.BorrowApproveRequest;
import com.example.library.dto.BorrowPageRequest;
import com.example.library.dto.BorrowReturnRequest;
import com.example.library.exception.BusinessException;
import com.example.library.result.Result;
import com.example.library.service.BorrowOrderService;
import com.example.library.vo.BorrowOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 借阅管理接口（馆员端）
 */
@Api(tags = "借阅管理（馆员端）")
@RestController
@RequestMapping("/librarian/borrow")
@Validated
@RequiredArgsConstructor
public class BorrowManageController {

    private final BorrowOrderService borrowOrderService;

    private void checkLibrarianRole(String role) {
        if (!"LIBRARIAN".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(403, "只有馆员或管理员可以执行该操作");
        }
    }

    /**
     * 审核借阅申请
     */
    @ApiOperation("审核借阅申请")
    @PostMapping("/approve")
    public Result<Void> approve(@RequestAttribute("currentUserId") Long currentUserId,
                                @RequestAttribute("currentUserRole") String currentUserRole,
                                @Valid @RequestBody BorrowApproveRequest request) {
        checkLibrarianRole(currentUserRole);
        borrowOrderService.approveBorrow(currentUserId, request);
        return Result.success();
    }

    /**
     * 办理出借
     */
    @ApiOperation("办理出借")
    @PostMapping("/lend/{id}")
    public Result<Void> lend(@RequestAttribute("currentUserId") Long currentUserId,
                             @RequestAttribute("currentUserRole") String currentUserRole,
                             @PathVariable("id") Long id) {
        checkLibrarianRole(currentUserRole);
        borrowOrderService.lendBook(currentUserId, id);
        return Result.success();
    }

    /**
     * 办理归还
     */
    @ApiOperation("办理归还")
    @PostMapping("/return")
    public Result<Void> returnBook(@RequestAttribute("currentUserId") Long currentUserId,
                                   @RequestAttribute("currentUserRole") String currentUserRole,
                                   @Valid @RequestBody BorrowReturnRequest request) {
        checkLibrarianRole(currentUserRole);
        borrowOrderService.returnBook(currentUserId, request);
        return Result.success();
    }

    /**
     * 馆员分页查询借阅记录
     */
    @ApiOperation("借阅记录分页查询")
    @GetMapping("/page")
    public Result<Page<BorrowOrderVO>> page(@RequestAttribute("currentUserRole") String currentUserRole,
                                            @Valid BorrowPageRequest request) {
        checkLibrarianRole(currentUserRole);
        Page<BorrowOrderVO> pageResult = borrowOrderService.pageBorrowOrders(request);
        return Result.success(pageResult);
    }
}

