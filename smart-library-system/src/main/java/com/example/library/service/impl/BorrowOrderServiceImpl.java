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
import com.example.library.vo.BorrowOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 借阅订单业务实现
 */
@Service
@RequiredArgsConstructor
public class BorrowOrderServiceImpl extends ServiceImpl<BorrowOrderMapper, BorrowOrder> implements BorrowOrderService {

    private final BookService bookService;
    private final UserService userService;

    @Override
    public void applyBorrow(Long userId, BorrowApplyRequest request) {
        // 校验图书是否存在
        Book book = bookService.getById(request.getBookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        // 校验图书是否上架
        if (book.getStatus() == null || book.getStatus() == 0) {
            throw new BusinessException("图书未上架或已下架");
        }
        // 校验可借库存是否大于 0
        if (book.getAvailableStock() == null || book.getAvailableStock() <= 0) {
            throw new BusinessException("图书库存不足");
        }
        // 一个用户对同一本书如果存在未完成借阅记录，则不允许重复申请
        long count = lambdaQuery()
                .eq(BorrowOrder::getUserId, userId)
                .eq(BorrowOrder::getBookId, request.getBookId())
                .in(BorrowOrder::getStatus,
                        BorrowStatusEnum.APPLYING.getCode(),
                        BorrowStatusEnum.APPROVED.getCode(),
                        BorrowStatusEnum.LENT.getCode(),
                        BorrowStatusEnum.OVERDUE.getCode())
                .count();
        if (count > 0) {
            throw new BusinessException("存在未完成的借阅记录，请先归还后再申请");
        }

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
        BorrowOrder order = getById(request.getOrderId());
        if (order == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (!BorrowStatusEnum.APPLYING.getCode().equals(order.getStatus())) {
            throw new BusinessException("当前状态不可审核");
        }

        order.setApproveTime(LocalDateTime.now());
        order.setApproveBy(librarianId);
        if (Boolean.TRUE.equals(request.getApprove())) {
            order.setStatus(BorrowStatusEnum.APPROVED.getCode());
        } else {
            order.setStatus(BorrowStatusEnum.REJECTED.getCode());
        }
        if (StringUtils.hasText(request.getRemark())) {
            order.setRemark(request.getRemark());
        }

        if (!updateById(order)) {
            throw new BusinessException("审核失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lendBook(Long librarianId, Long orderId) {
        BorrowOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (!BorrowStatusEnum.APPROVED.getCode().equals(order.getStatus())) {
            throw new BusinessException("只有已审核通过的记录才能办理出借");
        }

        Book book = bookService.getById(order.getBookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (book.getStatus() == null || book.getStatus() == 0) {
            throw new BusinessException("图书未上架或已下架");
        }
        if (book.getAvailableStock() == null || book.getAvailableStock() <= 0) {
            throw new BusinessException("图书库存不足，无法出借");
        }

        // 当前为单机基础版本，这里通过数据库条件更新简单防止超卖。
        // 后续可升级为使用版本号实现乐观锁，或基于 Redis/分布式锁的库存扣减方案。
        boolean updateSuccess = bookService.lambdaUpdate()
                .eq(Book::getId, book.getId())
                .gt(Book::getAvailableStock, 0)
                .setSql("available_stock = available_stock - 1, borrow_count = borrow_count + 1")
                .update();
        if (!updateSuccess) {
            throw new BusinessException("更新图书库存失败，请稍后重试");
        }

        // 更新借阅订单状态
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(BorrowStatusEnum.LENT.getCode());
        order.setLendTime(now);
        order.setDueTime(now.plusDays(30));

        if (!updateById(order)) {
            throw new BusinessException("办理出借失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnBook(Long librarianId, BorrowReturnRequest request) {
        BorrowOrder order = getById(request.getOrderId());
        if (order == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (!BorrowStatusEnum.LENT.getCode().equals(order.getStatus())
                && !BorrowStatusEnum.OVERDUE.getCode().equals(order.getStatus())) {
            throw new BusinessException("当前状态不可办理归还");
        }

        Book book = bookService.getById(order.getBookId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }

        // 恢复可借库存，防止重复归还通过借阅状态校验约束
        boolean updateSuccess = bookService.lambdaUpdate()
                .eq(Book::getId, book.getId())
                .setSql("available_stock = available_stock + 1")
                .update();
        if (!updateSuccess) {
            throw new BusinessException("更新图书库存失败，请稍后重试");
        }

        // 更新借阅订单状态
        order.setStatus(BorrowStatusEnum.RETURNED.getCode());
        order.setReturnTime(LocalDateTime.now());
        order.setReturnBy(librarianId);
        if (StringUtils.hasText(request.getRemark())) {
            order.setRemark(request.getRemark());
        }

        if (!updateById(order)) {
            throw new BusinessException("办理归还失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renewBorrow(Long userId, BorrowRenewRequest request) {
        BorrowOrder order = getById(request.getOrderId());
        if (order == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("无权操作该借阅记录");
        }
        if (!BorrowStatusEnum.LENT.getCode().equals(order.getStatus())) {
            throw new BusinessException("只有出借中的图书才能续借");
        }
        if (order.getRenewCount() != null && order.getRenewCount() >= 1) {
            throw new BusinessException("已达到最大续借次数");
        }
        if (order.getDueTime() == null) {
            throw new BusinessException("该借阅记录无应还时间，无法续借");
        }

        order.setDueTime(order.getDueTime().plusDays(15));
        order.setRenewCount(order.getRenewCount() == null ? 1 : order.getRenewCount() + 1);

        if (!updateById(order)) {
            throw new BusinessException("续借失败，请稍后重试");
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

        return buildOrderVOPage(orderPage);
    }

    @Override
    public Page<BorrowOrderVO> pageBorrowOrders(BorrowPageRequest request) {
        Page<BorrowOrder> page = new Page<>(request.getPageNum(), request.getPageSize());

        // 根据用户名筛选时，先查询用户ID列表
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

        return buildOrderVOPage(orderPage);
    }

    private Page<BorrowOrderVO> buildOrderVOPage(Page<BorrowOrder> orderPage) {
        List<BorrowOrder> records = orderPage.getRecords();
        if (records == null || records.isEmpty()) {
            Page<BorrowOrderVO> emptyPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 收集用户和图书ID
        Set<Long> userIds = records.stream()
                .map(BorrowOrder::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> bookIds = records.stream()
                .map(BorrowOrder::getBookId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        final Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        final Map<Long, Book> bookMap = bookIds.isEmpty()
                ? Collections.emptyMap()
                : bookService.listByIds(bookIds).stream().collect(Collectors.toMap(Book::getId, b -> b));

        List<BorrowOrderVO> voRecords = records.stream()
                .map(order -> toVO(order, userMap.get(order.getUserId()), bookMap.get(order.getBookId())))
                .collect(Collectors.toList());

        Page<BorrowOrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(voRecords);
        return voPage;
    }

    private BorrowOrderVO toVO(BorrowOrder order, User user, Book book) {
        BorrowOrderVO vo = new BorrowOrderVO();
        BeanUtils.copyProperties(order, vo);
        if (user != null) {
            vo.setUsername(user.getUsername());
        }
        if (book != null) {
            vo.setTitle(book.getTitle());
        }
        return vo;
    }
}

