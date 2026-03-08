package com.example.library.controller.admin;

import com.example.library.dto.SeatAddRequest;
import com.example.library.dto.SeatQueryRequest;
import com.example.library.dto.SeatUpdateRequest;
import com.example.library.exception.BusinessException;
import com.example.library.result.Result;
import com.example.library.service.SeatService;
import com.example.library.vo.SeatVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 座位管理接口（管理端）
 */
@Api(tags = "座位管理（管理端）")
@RestController
@RequestMapping("/admin/seat")
@Validated
@RequiredArgsConstructor
public class SeatAdminController {

    private final SeatService seatService;

    private void checkAdminRole(String role) {
        if (!"ADMIN".equals(role) && !"LIBRARIAN".equals(role)) {
            throw new BusinessException(403, "只有管理员或馆员可以执行该操作");
        }
    }

    @ApiOperation("新增座位")
    @PostMapping("/add")
    public Result<Void> add(@RequestAttribute("currentUserRole") String currentUserRole,
                            @Valid @RequestBody SeatAddRequest request) {
        checkAdminRole(currentUserRole);
        seatService.addSeat(request);
        return Result.success();
    }

    @ApiOperation("修改座位")
    @PostMapping("/update")
    public Result<Void> update(@RequestAttribute("currentUserRole") String currentUserRole,
                               @Valid @RequestBody SeatUpdateRequest request) {
        checkAdminRole(currentUserRole);
        seatService.updateSeat(request);
        return Result.success();
    }

    @ApiOperation("删除座位")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("currentUserRole") String currentUserRole,
                               @PathVariable("id") Long id) {
        checkAdminRole(currentUserRole);
        seatService.deleteSeat(id);
        return Result.success();
    }

    @ApiOperation("查询自习室下座位列表")
    @GetMapping("/list-by-room")
    public Result<List<SeatVO>> listByRoom(@RequestAttribute("currentUserRole") String currentUserRole,
                                           @Valid SeatQueryRequest request) {
        checkAdminRole(currentUserRole);
        List<SeatVO> list = seatService.listSeats(request);
        return Result.success(list);
    }
}

