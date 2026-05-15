package com.example.library.controller.admin;

import com.example.library.annotation.RequireRole;
import com.example.library.enums.UserRole;
import com.example.library.result.Result;
import com.example.library.service.DashboardService;
import com.example.library.vo.dashboard.DashboardOverviewVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台首页概览接口。
 */
@Tag(name = "支撑-Dashboard Overview")
@RestController
@RequestMapping("/admin/dashboard")
@Validated
@RequiredArgsConstructor
@RequireRole({UserRole.ADMIN, UserRole.LIBRARIAN})
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "后台首页概览：汇总借阅、预约、逾期和资源利用指标")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.overview());
    }
}
