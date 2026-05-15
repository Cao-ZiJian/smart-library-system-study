package com.example.library.controller.librarian;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.annotation.RequireRole;
import com.example.library.context.UserContext;
import com.example.library.dto.BorrowApproveRequest;
import com.example.library.dto.BorrowPageRequest;
import com.example.library.dto.BorrowReturnRequest;
import com.example.library.enums.UserRole;
import com.example.library.result.Result;
import com.example.library.service.BorrowOrderService;
import com.example.library.vo.BorrowOrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 借阅流程接口（馆员端）。
 */
@Tag(name = "核心-借阅流程（馆员端）")
@RestController
@RequestMapping("/librarian/borrow")
@Validated
@RequiredArgsConstructor
@RequireRole({UserRole.LIBRARIAN, UserRole.ADMIN})
public class BorrowManageController {

    private final BorrowOrderService borrowOrderService;

    @Operation(summary = "审核借阅申请：APPLYING 流转为 APPROVED 或 REJECTED")
    @PostMapping("/approve")
    public Result<Void> approve(@Valid @RequestBody BorrowApproveRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        borrowOrderService.approveBorrow(currentUserId, request);
        return Result.success();
    }

    @Operation(summary = "确认出借：状态流转为 LENT，并条件扣减库存")
    @PostMapping("/lend/{id}")
    public Result<Void> lend(@PathVariable("id") Long id) {
        Long currentUserId = UserContext.getRequiredUserId();
        borrowOrderService.lendBook(currentUserId, id);
        return Result.success();
    }

    @Operation(summary = "确认归还：LENT 或 OVERDUE 流转为 RETURNED，并恢复库存")
    @PostMapping("/return")
    public Result<Void> returnBook(@Valid @RequestBody BorrowReturnRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        borrowOrderService.returnBook(currentUserId, request);
        return Result.success();
    }

    @Operation(summary = "借阅流程记录分页：用于馆员处理申请、出借和归还")
    @GetMapping("/page")
    public Result<Page<BorrowOrderVO>> page(@Valid BorrowPageRequest request) {
        Page<BorrowOrderVO> pageResult = borrowOrderService.pageBorrowOrders(request);
        return Result.success(pageResult);
    }
}
