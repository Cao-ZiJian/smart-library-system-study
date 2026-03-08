package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.dto.ReservationCancelRequest;
import com.example.library.dto.ReservationCreateRequest;
import com.example.library.dto.ReservationFinishRequest;
import com.example.library.dto.ReservationPageRequest;
import com.example.library.dto.ReservationSignInRequest;
import com.example.library.entity.Reservation;
import com.example.library.entity.Seat;
import com.example.library.entity.StudyRoom;
import com.example.library.enums.ReservationStatusEnum;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.ReservationMapper;
import com.example.library.mapper.SeatMapper;
import com.example.library.mapper.StudyRoomMapper;
import com.example.library.service.ReservationService;
import com.example.library.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预约业务实现
 */
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    private final StudyRoomMapper studyRoomMapper;
    private final SeatMapper seatMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReservation(Long userId, ReservationCreateRequest request) {
        Seat seat = seatMapper.selectById(request.getSeatId());
        if (seat == null) {
            throw new BusinessException("座位不存在");
        }
        if (seat.getStatus() == null || seat.getStatus() == 0) {
            throw new BusinessException("座位不可用，无法预约");
        }

        StudyRoom room = studyRoomMapper.selectById(seat.getStudyRoomId());
        if (room == null || room.getStatus() == null || room.getStatus() == 0) {
            throw new BusinessException("自习室不存在或已被禁用");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException("开始时间必须早于结束时间");
        }
        if (!startTime.isAfter(now)) {
            throw new BusinessException("不能预约过去时间");
        }

        // 检查座位在该时间段内是否有冲突预约
        QueryWrapper<Reservation> seatConflictWrapper = new QueryWrapper<>();
        seatConflictWrapper.lambda()
                .eq(Reservation::getSeatId, seat.getId())
                .in(Reservation::getStatus,
                        ReservationStatusEnum.PENDING_CHECK_IN.getCode(),
                        ReservationStatusEnum.IN_USE.getCode())
                .lt(Reservation::getStartTime, endTime)
                .gt(Reservation::getEndTime, startTime);
        long seatConflictCount = count(seatConflictWrapper);
        if (seatConflictCount > 0) {
            throw new BusinessException("该时间段内座位已被预约");
        }

        // 检查用户在该时间段内是否已有有效预约
        QueryWrapper<Reservation> userConflictWrapper = new QueryWrapper<>();
        userConflictWrapper.lambda()
                .eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus,
                        ReservationStatusEnum.PENDING_CHECK_IN.getCode(),
                        ReservationStatusEnum.IN_USE.getCode())
                .lt(Reservation::getStartTime, endTime)
                .gt(Reservation::getEndTime, startTime);
        long userConflictCount = count(userConflictWrapper);
        if (userConflictCount > 0) {
            throw new BusinessException("该时间段内您已有其他预约");
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setSeatId(seat.getId());
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatusEnum.PENDING_CHECK_IN.getCode());
        reservation.setRemark(request.getRemark());

        if (!save(reservation)) {
            throw new BusinessException("创建预约失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long userId, ReservationCancelRequest request) {
        Reservation reservation = getById(request.getReservationId());
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!userId.equals(reservation.getUserId())) {
            throw new BusinessException("只能取消自己的预约");
        }
        if (!ReservationStatusEnum.PENDING_CHECK_IN.getCode().equals(reservation.getStatus())) {
            throw new BusinessException("当前状态不可取消");
        }

        reservation.setStatus(ReservationStatusEnum.CANCELED.getCode());
        if (!updateById(reservation)) {
            throw new BusinessException("取消预约失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signIn(Long userId, ReservationSignInRequest request) {
        Reservation reservation = getById(request.getReservationId());
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!userId.equals(reservation.getUserId())) {
            throw new BusinessException("只能签到自己的预约");
        }
        if (!ReservationStatusEnum.PENDING_CHECK_IN.getCode().equals(reservation.getStatus())) {
            throw new BusinessException("当前状态不可签到");
        }

        reservation.setStatus(ReservationStatusEnum.IN_USE.getCode());
        reservation.setCheckInTime(LocalDateTime.now());

        if (!updateById(reservation)) {
            throw new BusinessException("签到失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long userId, ReservationFinishRequest request) {
        Reservation reservation = getById(request.getReservationId());
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!userId.equals(reservation.getUserId())) {
            throw new BusinessException("只能结束自己的预约");
        }
        if (!ReservationStatusEnum.IN_USE.getCode().equals(reservation.getStatus())) {
            throw new BusinessException("当前状态不可结束使用");
        }

        reservation.setStatus(ReservationStatusEnum.FINISHED.getCode());
        reservation.setFinishTime(LocalDateTime.now());

        if (!updateById(reservation)) {
            throw new BusinessException("结束使用失败，请稍后重试");
        }
    }

    @Override
    public Page<ReservationVO> pageMyReservations(Long userId, ReservationPageRequest request) {
        Page<Reservation> page = new Page<>(request.getPageNum(), request.getPageSize());

        Page<Reservation> reservationPage = lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .eq(request.getStatus() != null, Reservation::getStatus, request.getStatus())
                .orderByDesc(Reservation::getStartTime)
                .page(page);

        List<Reservation> records = reservationPage.getRecords();
        if (records == null || records.isEmpty()) {
            Page<ReservationVO> emptyPage = new Page<>(reservationPage.getCurrent(), reservationPage.getSize(), reservationPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        Set<Long> seatIds = records.stream()
                .map(Reservation::getSeatId)
                .collect(Collectors.toSet());
        List<Seat> seats = seatMapper.selectBatchIds(seatIds);
        Map<Long, Seat> seatMap = seats.stream().collect(Collectors.toMap(Seat::getId, s -> s));

        Set<Long> roomIds = seats.stream()
                .map(Seat::getStudyRoomId)
                .collect(Collectors.toSet());
        List<StudyRoom> rooms = studyRoomMapper.selectBatchIds(roomIds);
        Map<Long, StudyRoom> roomMap = rooms.stream().collect(Collectors.toMap(StudyRoom::getId, r -> r));

        List<ReservationVO> voRecords = records.stream()
                .map(reservation -> toVO(reservation, seatMap.get(reservation.getSeatId()),
                        roomMap.get(seatMap.get(reservation.getSeatId()) != null
                                ? seatMap.get(reservation.getSeatId()).getStudyRoomId()
                                : null)))
                .collect(Collectors.toList());

        Page<ReservationVO> voPage = new Page<>(reservationPage.getCurrent(), reservationPage.getSize(), reservationPage.getTotal());
        voPage.setRecords(voRecords);
        return voPage;
    }

    private ReservationVO toVO(Reservation reservation, Seat seat, StudyRoom room) {
        ReservationVO vo = new ReservationVO();
        BeanUtils.copyProperties(reservation, vo);
        if (seat != null) {
            vo.setSeatId(seat.getId());
            vo.setSeatNumber(seat.getSeatNumber());
            vo.setStudyRoomId(seat.getStudyRoomId());
        }
        if (room != null) {
            vo.setStudyRoomName(room.getName());
        }
        return vo;
    }
}

