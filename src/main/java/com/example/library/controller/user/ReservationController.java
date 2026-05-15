package com.example.library.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.context.UserContext;
import com.example.library.dto.ReservationCancelRequest;
import com.example.library.dto.ReservationCreateRequest;
import com.example.library.dto.ReservationFinishRequest;
import com.example.library.dto.ReservationPageRequest;
import com.example.library.dto.ReservationSignInRequest;
import com.example.library.result.Result;
import com.example.library.service.ReservationService;
import com.example.library.vo.ReservationVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 座位预约流程接口。
 */
@Tag(name = "核心-座位预约")
@RestController
@RequestMapping("/user/reservation")
@Validated
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "创建座位预约：按用户和座位维度加锁，并校验时间冲突")
    @PostMapping("/create")
    public Result<Void> create(@Valid @RequestBody ReservationCreateRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        reservationService.createReservation(currentUserId, request);
        return Result.success();
    }

    @Operation(summary = "取消待签到预约：使用状态条件更新防重复操作")
    @PostMapping("/cancel")
    public Result<Void> cancel(@Valid @RequestBody ReservationCancelRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        reservationService.cancelReservation(currentUserId, request);
        return Result.success();
    }

    @Operation(summary = "预约签到：PENDING_CHECK_IN 流转为 IN_USE")
    @PostMapping("/sign-in")
    public Result<Void> signIn(@Valid @RequestBody ReservationSignInRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        reservationService.signIn(currentUserId, request);
        return Result.success();
    }

    @Operation(summary = "结束座位使用：IN_USE 流转为 FINISHED")
    @PostMapping("/finish")
    public Result<Void> finish(@Valid @RequestBody ReservationFinishRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        reservationService.finish(currentUserId, request);
        return Result.success();
    }

    @Operation(summary = "我的预约记录分页：查看待签到、使用中、已完成和过期状态")
    @GetMapping("/page")
    public Result<Page<ReservationVO>> page(@Valid ReservationPageRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        Page<ReservationVO> pageResult = reservationService.pageMyReservations(currentUserId, request);
        return Result.success(pageResult);
    }
}




















