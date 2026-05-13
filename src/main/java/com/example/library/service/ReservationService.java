package com.example.library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.ReservationCancelRequest;
import com.example.library.dto.ReservationCreateRequest;
import com.example.library.dto.ReservationFinishRequest;
import com.example.library.dto.ReservationPageRequest;
import com.example.library.dto.ReservationSignInRequest;
import com.example.library.entity.Reservation;
import com.example.library.vo.ReservationVO;

/**
 * 预约业务接口
 */
public interface ReservationService extends IService<Reservation> {

    /**
     * 创建预约
     */
    void createReservation(Long userId, ReservationCreateRequest request);

    /**
     * 取消预约
     */
    void cancelReservation(Long userId, ReservationCancelRequest request);

    /**
     * 签到
     */
    void signIn(Long userId, ReservationSignInRequest request);

    /**
     * 结束使用
     */
    void finish(Long userId, ReservationFinishRequest request);

    /**
     * 用户分页查询自己的预约记录
     */
    Page<ReservationVO> pageMyReservations(Long userId, ReservationPageRequest request);
}

