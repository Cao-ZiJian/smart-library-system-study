package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 馆员审核借阅请求参数
 */
@Data
public class BorrowApproveRequest {

    /**
     * 借阅订单ID
     */
    @NotNull(message = "借阅订单ID不能为空")
    private Long orderId;

    /**
     * 是否通过，true 表示通过，false 表示拒绝
     */
    @NotNull(message = "审核结果不能为空")
    private Boolean approve;

    /**
     * 备注（拒绝时建议填写原因）
     */
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}

