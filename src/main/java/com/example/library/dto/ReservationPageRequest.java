package com.example.library.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 预约记录分页查询请求参数
 */
@Data
public class ReservationPageRequest {

    /**
     * 当前页码，从 1 开始
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须从 1 开始")
    private Integer pageNum;

    /**
     * 每页数量
     */
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于 0")
    @Max(value = 100, message = "每页数量不能超过 100")
    private Integer pageSize;

    /**
     * 状态，可选
     */
    @Size(max = 20, message = "预约状态长度不能超过20")
    @Pattern(regexp = "PENDING_CHECK_IN|CANCELED|IN_USE|FINISHED|EXPIRED",
            message = "预约状态不合法")
    private String status;
}

