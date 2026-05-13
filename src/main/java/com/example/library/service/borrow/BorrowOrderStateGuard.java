package com.example.library.service.borrow;

import com.example.library.entity.BorrowOrder;
import com.example.library.enums.BorrowStatusEnum;
import com.example.library.exception.BusinessException;

/**
 * 借阅单状态机校验（集中表达合法跳转，避免 Controller/Service 散落 if）
 */
public final class BorrowOrderStateGuard {

    private BorrowOrderStateGuard() {
    }

    public static void requireApplying(BorrowOrder order) {
        requireStatus(order, BorrowStatusEnum.APPLYING, "当前状态不可审核");
    }

    public static void requireApproved(BorrowOrder order) {
        requireStatus(order, BorrowStatusEnum.APPROVED, "只有已审核通过的记录才能办理出借");
    }

    public static void requireLentOrOverdue(BorrowOrder order) {
        if (order == null) {
            throw new BusinessException("借阅记录不存在");
        }
        String s = order.getStatus();
        if (!BorrowStatusEnum.LENT.getCode().equals(s) && !BorrowStatusEnum.OVERDUE.getCode().equals(s)) {
            throw new BusinessException("当前状态不可办理归还");
        }
    }

    public static void requireLent(BorrowOrder order) {
        requireStatus(order, BorrowStatusEnum.LENT, "只有出借中的图书才能续借");
    }

    private static void requireStatus(BorrowOrder order, BorrowStatusEnum expected, String message) {
        if (order == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (!expected.getCode().equals(order.getStatus())) {
            throw new BusinessException(message);
        }
    }
}
