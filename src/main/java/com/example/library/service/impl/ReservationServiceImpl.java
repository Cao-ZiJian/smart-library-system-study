package com.example.library.service.impl;

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
import com.example.library.service.reservation.ReservationAssembler;
import com.example.library.service.reservation.ReservationConflictChecker;
import com.example.library.service.reservation.ReservationLockExecutor;
import com.example.library.service.reservation.ReservationStateGuard;
import com.example.library.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/**
 * 预约业务实现（创建预约使用 Redis 锁消除「先查后插」竞态）
 */
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    private final StudyRoomMapper studyRoomMapper;
    private final SeatMapper seatMapper;
    private final ReservationLockExecutor reservationLockExecutor;
    private final ReservationConflictChecker reservationConflictChecker;
    private final ReservationStateGuard reservationStateGuard;
    private final ReservationAssembler reservationAssembler;
    private final TransactionTemplate transactionTemplate;

    @Override
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
        reservationStateGuard.validateCreateTimeRange(startTime, endTime, now);

        reservationLockExecutor.executeForCreate(userId, seat.getId(), () -> {
            transactionTemplate.executeWithoutResult(status ->
                    doCreateReservation(userId, seat.getId(), startTime, endTime, request.getRemark()));
            return null;
        });
    }

    private void doCreateReservation(Long userId, Long seatId,
                                     LocalDateTime startTime, LocalDateTime endTime, String remark) {
        reservationConflictChecker.assertNoSeatConflict(seatId, startTime, endTime);
        reservationConflictChecker.assertNoUserConflict(userId, startTime, endTime);

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setSeatId(seatId);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatusEnum.PENDING_CHECK_IN.getCode());
        reservation.setRemark(remark);

        if (!save(reservation)) {
            throw new BusinessException("创建预约失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long userId, ReservationCancelRequest request) {
        Reservation reservation = getById(request.getReservationId());
        reservationStateGuard.requireCancelable(reservation, userId);

        boolean updated = lambdaUpdate()
                .eq(Reservation::getId, request.getReservationId())
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING_CHECK_IN.getCode())
                .set(Reservation::getStatus, ReservationStatusEnum.CANCELED.getCode())
                .update();
        if (!updated) {
            throw new BusinessException("取消预约失败：预约状态已变更，请勿重复操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signIn(Long userId, ReservationSignInRequest request) {
        Reservation reservation = getById(request.getReservationId());
        reservationStateGuard.requireSignInable(reservation, userId);

        boolean updated = lambdaUpdate()
                .eq(Reservation::getId, request.getReservationId())
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getStatus, ReservationStatusEnum.PENDING_CHECK_IN.getCode())
                .set(Reservation::getStatus, ReservationStatusEnum.IN_USE.getCode())
                .set(Reservation::getCheckInTime, LocalDateTime.now())
                .update();
        if (!updated) {
            throw new BusinessException("签到失败：预约状态已变更，请勿重复操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long userId, ReservationFinishRequest request) {
        Reservation reservation = getById(request.getReservationId());
        reservationStateGuard.requireFinishable(reservation, userId);

        boolean updated = lambdaUpdate()
                .eq(Reservation::getId, request.getReservationId())
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getStatus, ReservationStatusEnum.IN_USE.getCode())
                .set(Reservation::getStatus, ReservationStatusEnum.FINISHED.getCode())
                .set(Reservation::getFinishTime, LocalDateTime.now())
                .update();
        if (!updated) {
            throw new BusinessException("结束使用失败：预约状态已变更，请勿重复操作");
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

        return reservationAssembler.toPageVO(reservationPage);
    }
}
