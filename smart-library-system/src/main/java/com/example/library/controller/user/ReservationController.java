package com.example.library.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.dto.ReservationCancelRequest;
import com.example.library.dto.ReservationCreateRequest;
import com.example.library.dto.ReservationFinishRequest;
import com.example.library.dto.ReservationPageRequest;
import com.example.library.dto.ReservationSignInRequest;
import com.example.library.result.Result;
import com.example.library.service.ReservationService;
import com.example.library.vo.ReservationVO;
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
 * 预约接口（用户端）
 */
@Api(tags = "预约模块（用户端）")
@RestController
@RequestMapping("/user/reservation")
@Validated
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @ApiOperation("创建预约")
    @PostMapping("/create")
    public Result<Void> create(@RequestAttribute("currentUserId") Long currentUserId,
                               @Valid @RequestBody ReservationCreateRequest request) {
        reservationService.createReservation(currentUserId, request);
        return Result.success();
    }

    @ApiOperation("取消预约")
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestAttribute("currentUserId") Long currentUserId,
                               @Valid @RequestBody ReservationCancelRequest request) {
        reservationService.cancelReservation(currentUserId, request);
        return Result.success();
    }

    @ApiOperation("预约签到")
    @PostMapping("/sign-in")
    public Result<Void> signIn(@RequestAttribute("currentUserId") Long currentUserId,
                               @Valid @RequestBody ReservationSignInRequest request) {
        reservationService.signIn(currentUserId, request);
        return Result.success();
    }

    @ApiOperation("结束使用")
    @PostMapping("/finish")
    public Result<Void> finish(@RequestAttribute("currentUserId") Long currentUserId,
                               @Valid @RequestBody ReservationFinishRequest request) {
        reservationService.finish(currentUserId, request);
        return Result.success();
    }

    @ApiOperation("分页查询我的预约记录")
    @GetMapping("/page")
    public Result<Page<ReservationVO>> page(@RequestAttribute("currentUserId") Long currentUserId,
                                            @Valid ReservationPageRequest request) {
        Page<ReservationVO> pageResult = reservationService.pageMyReservations(currentUserId, request);
        return Result.success(pageResult);
    }
}

