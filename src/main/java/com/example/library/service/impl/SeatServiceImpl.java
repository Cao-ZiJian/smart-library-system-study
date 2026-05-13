package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.dto.SeatAddRequest;
import com.example.library.dto.SeatQueryRequest;
import com.example.library.dto.SeatUpdateRequest;
import com.example.library.entity.Reservation;
import com.example.library.entity.Seat;
import com.example.library.entity.StudyRoom;
import com.example.library.enums.ReservationStatusEnum;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.ReservationMapper;
import com.example.library.mapper.SeatMapper;
import com.example.library.mapper.StudyRoomMapper;
import com.example.library.service.SeatService;
import com.example.library.vo.SeatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 座位业务实现
 */
@Service
@RequiredArgsConstructor
public class SeatServiceImpl extends ServiceImpl<SeatMapper, Seat> implements SeatService {

    private final StudyRoomMapper studyRoomMapper;
    private final ReservationMapper reservationMapper;

    @Override
    public void addSeat(SeatAddRequest request) {
        StudyRoom room = studyRoomMapper.selectById(request.getStudyRoomId());
        if (room == null) {
            throw new BusinessException("自习室不存在");
        }

        // 同一自习室内座位编号唯一
        long count = lambdaQuery()
                .eq(Seat::getStudyRoomId, request.getStudyRoomId())
                .eq(Seat::getSeatNumber, request.getSeatNumber())
                .count();
        if (count > 0) {
            throw new BusinessException("同一自习室内座位编号已存在");
        }

        Seat seat = new Seat();
        seat.setStudyRoomId(request.getStudyRoomId());
        seat.setSeatNumber(request.getSeatNumber());
        seat.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        try {
            if (!save(seat)) {
                throw new BusinessException("新增座位失败，请稍后重试");
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("同一自习室内座位编号已存在");
        }
    }

    @Override
    public void updateSeat(SeatUpdateRequest request) {
        Seat seat = getById(request.getId());
        if (seat == null) {
            throw new BusinessException("座位不存在");
        }

        StudyRoom room = studyRoomMapper.selectById(request.getStudyRoomId());
        if (room == null) {
            throw new BusinessException("自习室不存在");
        }

        // 同一自习室内座位编号唯一（排除自身）
        long count = lambdaQuery()
                .eq(Seat::getStudyRoomId, request.getStudyRoomId())
                .eq(Seat::getSeatNumber, request.getSeatNumber())
                .ne(Seat::getId, request.getId())
                .count();
        if (count > 0) {
            throw new BusinessException("同一自习室内座位编号已存在");
        }

        seat.setStudyRoomId(request.getStudyRoomId());
        seat.setSeatNumber(request.getSeatNumber());
        if (request.getStatus() != null) {
            seat.setStatus(request.getStatus());
        }

        try {
            if (!updateById(seat)) {
                throw new BusinessException("修改座位失败，请稍后重试");
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("同一自习室内座位编号已存在");
        }
    }

    @Override
    public void deleteSeat(Long id) {
        if (id == null) {
            throw new BusinessException("座位ID不能为空");
        }
        // 存在未来有效预约（待签到或使用中，且结束时间晚于当前）则不允许删除
        long futureReservationCount = reservationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Reservation>()
                        .eq("seat_id", id)
                        .in("status",
                                ReservationStatusEnum.PENDING_CHECK_IN.getCode(),
                                ReservationStatusEnum.IN_USE.getCode())
                        .gt("end_time", LocalDateTime.now())
        );
        if (futureReservationCount > 0) {
            throw new BusinessException("该座位存在未结束的预约，无法删除");
        }
        if (!removeById(id)) {
            throw new BusinessException("座位不存在或已被删除");
        }
    }

    @Override
    public List<SeatVO> listSeats(SeatQueryRequest request) {
        List<Seat> list = lambdaQuery()
                .eq(Seat::getStudyRoomId, request.getStudyRoomId())
                .eq(request.getStatus() != null, Seat::getStatus, request.getStatus())
                .orderByAsc(Seat::getSeatNumber)
                .list();

        return list.stream()
                .map(seat -> {
                    SeatVO vo = new SeatVO();
                    BeanUtils.copyProperties(seat, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }
}

