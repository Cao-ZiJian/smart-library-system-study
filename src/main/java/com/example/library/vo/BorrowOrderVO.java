package com.example.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅订单对外展示对象
 */
@Data
public class BorrowOrderVO {

    /**
     * 借阅ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 图书标题
     */
    private String title;

    /**
     * 状态
     */
    private String status;

    /**
     * 申请时间
     */
    private LocalDateTime applyTime;

    /**
     * 审核时间
     */
    private LocalDateTime approveTime;

    /**
     * 出借时间
     */
    private LocalDateTime lendTime;

    /**
     * 应还时间
     */
    private LocalDateTime dueTime;

    /**
     * 归还时间
     */
    private LocalDateTime returnTime;

    /**
     * 续借次数
     */
    private Integer renewCount;

    /**
     * 备注
     */
    private String remark;
}

