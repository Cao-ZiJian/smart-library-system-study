package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.Reservation;
import org.springframework.stereotype.Repository;

/**
 * 预约 Mapper
 */
@Repository
public interface ReservationMapper extends BaseMapper<Reservation> {
}

