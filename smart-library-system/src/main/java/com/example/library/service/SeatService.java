package com.example.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.SeatAddRequest;
import com.example.library.dto.SeatQueryRequest;
import com.example.library.dto.SeatUpdateRequest;
import com.example.library.entity.Seat;
import com.example.library.vo.SeatVO;

import java.util.List;

/**
 * 座位业务接口
 */
public interface SeatService extends IService<Seat> {

    /**
     * 新增座位
     */
    void addSeat(SeatAddRequest request);

    /**
     * 修改座位
     */
    void updateSeat(SeatUpdateRequest request);

    /**
     * 删除座位
     */
    void deleteSeat(Long id);

    /**
     * 查询某自习室下座位列表
     */
    List<SeatVO> listSeats(SeatQueryRequest request);
}

