package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.dto.BorrowApplyRequest;
import com.example.library.dto.BorrowApproveRequest;
import com.example.library.dto.BorrowPageRequest;
import com.example.library.dto.BorrowRenewRequest;
import com.example.library.dto.BorrowReturnRequest;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowOrder;
import com.example.library.entity.User;
import com.example.library.enums.BorrowStatusEnum;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.BorrowOrderMapper;
import com.example.library.service.BookService;
import com.example.library.service.BorrowOrderService;
import com.example.library.service.UserService;
import com.example.library.service.borrow.BorrowApplyLockExecutor;
import com.example.library.service.borrow.BorrowInventoryService;
import com.example.library.service.borrow.BorrowOrderAssembler;
import com.example.library.service.borrow.BorrowOrderStateGuard;
import com.example.library.service.borrow.BorrowOrderValidator;
import com.example.library.vo.BorrowOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 借阅订单业务实现（状态机 + 条件更新防并发重复处理）
 */
@Service
@RequiredArgsConstructor
public class BorrowOrderServiceImpl extends ServiceImpl<BorrowOrderMapper, BorrowOrder> implements BorrowOrderService {

    private final BookService bookService;
    private final UserService userService;
    private final BorrowApplyLockExecutor borrowApplyLockExecutor;
    private final BorrowInventoryService borrowInventoryService;
    private final BorrowOrderValidator borrowOrderValidator;
    private final BorrowOrderAssembler borrowOrderAssembler;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void applyBorrow(Long userId, BorrowApplyRequest request) {
        borrowApplyLockExecutor.execute(userId, request.getBookId(), () -> {
            transactionTemplate.executeWithoutResult(status -> doApplyBorrow(userId, request));
            return null;
        });
    }

    /**
     * 申请阶段只创建申请单，不扣减库存；真正库存扣减仍在出借阶段通过条件更新完成。
     * 事务放在分布式锁内部，确保申请单提交完成后再释放锁，避免同一用户同书重复申请的短暂可见性窗口。
     */
    private void doApplyBorrow(Long userId, BorrowApplyRequest request) {
        borrowInventoryService.requireBookForApply(request.getBookId());
        borrowOrderValidator.validateApply(userId, request);

        BorrowOrder order = new BorrowOrder();
        order.setUserId(userId);
        order.setBookId(request.getBookId());
        order.setStatus(BorrowStatusEnum.APPLYING.getCode());
        order.setApplyTime(LocalDateTime.now());
        order.setRenewCount(0);
        order.setRemark(request.getRemark());

        if (!save(order)) {
            throw new BusinessException("借阅申请失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBorrow(Long librarianId, BorrowApproveRequest request) {
        BorrowOrder probe = getById(request.getOrderId());
        if (probe == null) {
            throw new BusinessException("借阅记录不存在");
        }
        BorrowOrderStateGuard.requireApplying(probe);

        String nextStatus = Boolean.TRUE.equals(request.getApprove())
                ? BorrowStatusEnum.APPROVED.getCode()
                : BorrowStatusEnum.REJECTED.getCode();

        boolean updated = lambdaUpdate()
                .eq(BorrowOrder::getId, request.getOrderId())
                .eq(BorrowOrder::getStatus, BorrowStatusEnum.APPLYING.getCode())
                .set(BorrowOrder::getApproveTime, LocalDateTime.now())
                .set(BorrowOrder::getApproveBy, librarianId)
                .set(BorrowOrder::getStatus, nextStatus)
                .set(StringUtils.hasText(request.getRemark()), BorrowOrder::getRemark, request.getRemark())
                .update();
        if (!updated) {
            throw new BusinessException("审核失败：订单状态已变更，请勿重复处理");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lendBook(Long librarianId, Long orderId) {
        BorrowOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("借阅记录不存在");
        }
        BorrowOrderStateGuard.requireApproved(order);

        Book book = borrowInventoryService.requireBookForLend(order.getBookId());

        // 先条件更新订单，避免同一单被并发重复出借；失败则无需动库存
        LocalDateTime now = LocalDateTime.now();
        boolean orderLocked = lambdaUpdate()
                .eq(BorrowOrder::getId, orderId)
                .eq(BorrowOrder::getStatus, BorrowStatusEnum.APPROVED.getCode())
                .set(BorrowOrder::getStatus, BorrowStatusEnum.LENT.getCode())
                .set(BorrowOrder::getLendTime, now)
                .set(BorrowOrder::getDueTime, now.plusDays(30))
                .update();
        if (!orderLocked) {
            throw new BusinessException("办理出借失败：订单不是「已通过」状态或已被出借");
        }
        borrowInventoryService.decreaseAvailableStock(book.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnBook(Long librarianId, BorrowReturnRequest request) {
        BorrowOrder probe = getById(request.getOrderId());
        if (probe == null) {
            throw new BusinessException("借阅记录不存在");
        }
        BorrowOrderStateGuard.requireLentOrOverdue(probe);

        boolean orderUpdated = lambdaUpdate()
                .eq(BorrowOrder::getId, request.getOrderId())
                .in(BorrowOrder::getStatus,
                        BorrowStatusEnum.LENT.getCode(),
                        BorrowStatusEnum.OVERDUE.getCode())
                .set(BorrowOrder::getStatus, BorrowStatusEnum.RETURNED.getCode())
                .set(BorrowOrder::getReturnTime, LocalDateTime.now())
                .set(BorrowOrder::getReturnBy, librarianId)
                .set(StringUtils.hasText(request.getRemark()), BorrowOrder::getRemark, request.getRemark())
                .update();
        if (!orderUpdated) {
            throw new BusinessException("归还失败：订单状态已变更，请勿重复归还");
        }

        borrowInventoryService.requireExistingBook(probe.getBookId());
        borrowInventoryService.increaseAvailableStock(probe.getBookId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renewBorrow(Long userId, BorrowRenewRequest request) {
        BorrowOrder probe = getById(request.getOrderId());
        if (probe == null) {
            throw new BusinessException("借阅记录不存在");
        }
        borrowOrderValidator.validateRenewOwnership(userId, probe);
        BorrowOrderStateGuard.requireLent(probe);
        if (probe.getRenewCount() != null && probe.getRenewCount() >= 1) {
            throw new BusinessException("已达到最大续借次数");
        }
        if (probe.getDueTime() == null) {
            throw new BusinessException("该借阅记录无应还时间，无法续借");
        }

        boolean ok = lambdaUpdate()
                .eq(BorrowOrder::getId, request.getOrderId())
                .eq(BorrowOrder::getUserId, userId)
                .eq(BorrowOrder::getStatus, BorrowStatusEnum.LENT.getCode())
                .and(
                        w ->
                                w.isNull(BorrowOrder::getRenewCount).or().lt(BorrowOrder::getRenewCount, 1))
                .setSql("due_time = DATE_ADD(due_time, INTERVAL 15 DAY), renew_count = IFNULL(renew_count,0) + 1")
                .update();
        if (!ok) {
            throw new BusinessException("续借失败：状态或续借次数已变更，请勿重复提交");
        }
    }

    @Override
    public Page<BorrowOrderVO> pageUserBorrowOrders(Long userId, BorrowPageRequest request) {
        Page<BorrowOrder> page = new Page<>(request.getPageNum(), request.getPageSize());

        Page<BorrowOrder> orderPage = lambdaQuery()
                .eq(BorrowOrder::getUserId, userId)
                .eq(request.getBookId() != null, BorrowOrder::getBookId, request.getBookId())
                .eq(StringUtils.hasText(request.getStatus()), BorrowOrder::getStatus, request.getStatus())
                .orderByDesc(BorrowOrder::getApplyTime)
                .page(page);

        return borrowOrderAssembler.buildOrderVOPage(orderPage);
    }

    @Override
    public Page<BorrowOrderVO> pageBorrowOrders(BorrowPageRequest request) {
        Page<BorrowOrder> page = new Page<>(request.getPageNum(), request.getPageSize());

        List<Long> userIdList = null;
        if (StringUtils.hasText(request.getUsername())) {
            List<User> users = userService.lambdaQuery()
                    .like(User::getUsername, request.getUsername())
                    .list();
            if (CollectionUtils.isEmpty(users)) {
                Page<BorrowOrderVO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), 0);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
            userIdList = users.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
        }

        Page<BorrowOrder> orderPage = lambdaQuery()
                .eq(request.getUserId() != null, BorrowOrder::getUserId, request.getUserId())
                .eq(request.getBookId() != null, BorrowOrder::getBookId, request.getBookId())
                .eq(StringUtils.hasText(request.getStatus()), BorrowOrder::getStatus, request.getStatus())
                .in(userIdList != null, BorrowOrder::getUserId, userIdList)
                .orderByDesc(BorrowOrder::getApplyTime)
                .page(page);

        return borrowOrderAssembler.buildOrderVOPage(orderPage);
    }
}
