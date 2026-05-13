package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借阅订单实体，对应表 borrow_order
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("borrow_order")
public class BorrowOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 状态：APPLYING/APPROVED/LENT/RETURNED/OVERDUE/REJECTED
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
     * 审核人（馆员）
     */
    private Long approveBy;

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
     * 办理归还的馆员
     */
    private Long returnBy;

    /**
     * 续借次数
     */
    private Integer renewCount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

