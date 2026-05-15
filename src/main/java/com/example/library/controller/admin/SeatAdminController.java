package com.example.library.controller.admin;

import com.example.library.annotation.RequireRole;
import com.example.library.dto.SeatAddRequest;
import com.example.library.dto.SeatQueryRequest;
import com.example.library.dto.SeatUpdateRequest;
import com.example.library.enums.UserRole;
import com.example.library.result.Result;
import com.example.library.service.SeatService;
import com.example.library.vo.SeatVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 座位基础数据维护接口，作为预约资源支撑。
 */
@Tag(name = "支撑-座位基础数据")
@RestController
@RequestMapping("/admin/seat")
@Validated
@RequiredArgsConstructor
@RequireRole({UserRole.ADMIN, UserRole.LIBRARIAN})
public class SeatAdminController {

    private final SeatService seatService;

    @Operation(summary = "录入座位资源：为自习室配置可预约座位")
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody SeatAddRequest request) {
        seatService.addSeat(request);
        return Result.success();
    }

    @Operation(summary = "维护座位资源：调整座位编号和可用状态")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody SeatUpdateRequest request) {
        seatService.updateSeat(request);
        return Result.success();
    }

    @Operation(summary = "清理无未结束预约的座位资源")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        seatService.deleteSeat(id);
        return Result.success();
    }

    @Operation(summary = "自习室座位列表：支撑预约资源核对")
    @GetMapping("/list-by-room")
    public Result<List<SeatVO>> listByRoom(@Valid SeatQueryRequest request) {
        List<SeatVO> list = seatService.listSeats(request);
        return Result.success(list);
    }
}
