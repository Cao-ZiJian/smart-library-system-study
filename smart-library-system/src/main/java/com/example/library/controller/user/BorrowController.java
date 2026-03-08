package com.example.library.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 借阅模块接口（用户端）
 */
@Api(tags = "借阅模块（用户端）")
@RestController
@RequestMapping("/user/borrow")
@Validated
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowOrderService borrowOrderService;

    /**
     * 用户申请借阅
     */
    @ApiOperation("用户申请借阅")
    @PostMapping("/apply")
    public Result<Void> apply(@RequestAttribute("currentUserId") Long currentUserId,
                              @Valid @RequestBody BorrowApplyRequest request) {
        borrowOrderService.applyBorrow(currentUserId, request);
        return Result.success();
    }

    /**
     * 用户续借
     */
    @ApiOperation("用户续借")
    @PostMapping("/renew")
    public Result<Void> renew(@RequestAttribute("currentUserId") Long currentUserId,
                              @Valid @RequestBody BorrowRenewRequest request) {
        borrowOrderService.renewBorrow(currentUserId, request);
        return Result.success();
    }

    /**
     * 用户分页查询自己的借阅记录
     */
    @ApiOperation("查询我的借阅记录（分页）")
    @GetMapping("/page")
    public Result<Page<BorrowOrderVO>> page(@RequestAttribute("currentUserId") Long currentUserId,
                                            @Valid BorrowPageRequest request) {
        Page<BorrowOrderVO> pageResult = borrowOrderService.pageUserBorrowOrders(currentUserId, request);
        return Result.success(pageResult);
    }
}

