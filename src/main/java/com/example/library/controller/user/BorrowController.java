package com.example.library.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.context.UserContext;
import com.example.library.dto.BorrowApplyRequest;
import com.example.library.dto.BorrowPageRequest;
import com.example.library.dto.BorrowRenewRequest;
import com.example.library.result.Result;
import com.example.library.service.BorrowOrderService;
import com.example.library.vo.BorrowOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 借阅流程接口（用户端）。
 */
@Api(tags = "核心-借阅流程（用户端）")
@RestController
@RequestMapping("/user/borrow")
@Validated
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowOrderService borrowOrderService;

    /**
     * 用户申请借阅
     */
    @ApiOperation("提交借阅申请：按用户和图书维度加锁，申请阶段不扣库存")
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody BorrowApplyRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        borrowOrderService.applyBorrow(currentUserId, request);
        return Result.success();
    }

    /**
     * 用户续借
     */
    @ApiOperation("续借出借中图书：校验本人、状态和续借次数")
    @PostMapping("/renew")
    public Result<Void> renew(@Valid @RequestBody BorrowRenewRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        borrowOrderService.renewBorrow(currentUserId, request);
        return Result.success();
    }

    /**
     * 用户分页查询自己的借阅记录
     */
    @ApiOperation("我的借阅记录分页：查看申请、出借、逾期和归还状态")
    @GetMapping("/page")
    public Result<Page<BorrowOrderVO>> page(@Valid BorrowPageRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        Page<BorrowOrderVO> pageResult =
                borrowOrderService.pageUserBorrowOrders(currentUserId, request);
        return Result.success(pageResult);
    }
}
