package com.example.library.service.borrow;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.library.dto.BorrowApplyRequest;
import com.example.library.entity.BorrowOrder;
import com.example.library.enums.BorrowStatusEnum;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.BorrowOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 借阅业务前置校验
 */
@Component
@RequiredArgsConstructor
public class BorrowOrderValidator {

    private final BorrowOrderMapper borrowOrderMapper;

    public void validateApply(Long userId, BorrowApplyRequest request) {
        QueryWrapper<BorrowOrder> activeOrderQuery = new QueryWrapper<>();
        activeOrderQuery.lambda()
                .eq(BorrowOrder::getUserId, userId)
                .eq(BorrowOrder::getBookId, request.getBookId())
                .in(BorrowOrder::getStatus,
                        BorrowStatusEnum.APPLYING.getCode(),
                        BorrowStatusEnum.APPROVED.getCode(),
                        BorrowStatusEnum.LENT.getCode(),
                        BorrowStatusEnum.OVERDUE.getCode());
        long count = borrowOrderMapper.selectCount(activeOrderQuery);
        if (count > 0) {
            throw new BusinessException("存在未完成的借阅记录，请先归还后再申请");
        }
    }

    public void validateRenewOwnership(Long userId, BorrowOrder order) {
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("无权操作该借阅记录");
        }
    }
}
