package com.example.library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.BorrowApplyRequest;
import com.example.library.dto.BorrowApproveRequest;
import com.example.library.dto.BorrowPageRequest;
import com.example.library.dto.BorrowRenewRequest;
import com.example.library.dto.BorrowReturnRequest;
import com.example.library.entity.BorrowOrder;
import com.example.library.vo.BorrowOrderVO;

/**
 * 借阅订单业务接口
 */
public interface BorrowOrderService extends IService<BorrowOrder> {

    /**
     * 用户申请借阅
     *
     * @param userId  当前用户ID
     * @param request 请求参数
     */
    void applyBorrow(Long userId, BorrowApplyRequest request);

    /**
     * 馆员审核借阅申请
     *
     * @param librarianId 馆员ID
     * @param request     请求参数
     */
    void approveBorrow(Long librarianId, BorrowApproveRequest request);

    /**
     * 办理出借
     *
     * @param librarianId 馆员ID
     * @param orderId     借阅订单ID
     */
    void lendBook(Long librarianId, Long orderId);

    /**
     * 办理归还
     *
     * @param librarianId 馆员ID
     * @param request     请求参数
     */
    void returnBook(Long librarianId, BorrowReturnRequest request);

    /**
     * 用户续借
     *
     * @param userId  当前用户ID
     * @param request 请求参数
     */
    void renewBorrow(Long userId, BorrowRenewRequest request);

    /**
     * 用户端分页查询自己的借阅记录
     *
     * @param userId  当前用户ID
     * @param request 查询参数
     * @return 借阅记录分页
     */
    Page<BorrowOrderVO> pageUserBorrowOrders(Long userId, BorrowPageRequest request);

    /**
     * 馆员端分页查询借阅记录
     *
     * @param request 查询参数
     * @return 借阅记录分页
     */
    Page<BorrowOrderVO> pageBorrowOrders(BorrowPageRequest request);
}

