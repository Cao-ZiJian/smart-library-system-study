package com.example.library.service.reservation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.entity.Reservation;
import com.example.library.entity.Seat;
import com.example.library.entity.StudyRoom;
import com.example.library.mapper.SeatMapper;
import com.example.library.mapper.StudyRoomMapper;
import com.example.library.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预约记录 VO 组装
 */
@Component
@RequiredArgsConstructor
public class ReservationAssembler {

    private final SeatMapper seatMapper;
    private final StudyRoomMapper studyRoomMapper;

    public Page<ReservationVO> toPageVO(Page<Reservation> reservationPage) {
        List<Reservation> records = reservationPage.getRecords();
        if (records == null || records.isEmpty()) {
            Page<ReservationVO> emptyPage = new Page<>(reservationPage.getCurrent(),
                    reservationPage.getSize(), reservationPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        Set<Long> seatIds = records.stream()
                .map(Reservation::getSeatId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Seat> seats = seatIds.isEmpty() ? Collections.emptyList() : seatMapper.selectBatchIds(seatIds);
        Map<Long, Seat> seatMap = seats == null || seats.isEmpty()
                ? Collections.emptyMap()
                : seats.stream()
                .filter(seat -> seat.getId() != null)
                .collect(Collectors.toMap(Seat::getId, seat -> seat));

        Set<Long> roomIds = seats.stream()
                .map(Seat::getStudyRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<StudyRoom> rooms = roomIds.isEmpty() ? Collections.emptyList() : studyRoomMapper.selectBatchIds(roomIds);
        Map<Long, StudyRoom> roomMap = rooms == null || rooms.isEmpty()
                ? Collections.emptyMap()
                : rooms.stream()
                .filter(room -> room.getId() != null)
                .collect(Collectors.toMap(StudyRoom::getId, room -> room));

        List<ReservationVO> voRecords = records.stream()
                .map(reservation -> {
                    Seat seat = seatMap.get(reservation.getSeatId());
                    StudyRoom room = seat == null ? null : roomMap.get(seat.getStudyRoomId());
                    return toVO(reservation, seat, room);
                })
                .collect(Collectors.toList());

        Page<ReservationVO> voPage = new Page<>(reservationPage.getCurrent(),
                reservationPage.getSize(), reservationPage.getTotal());
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
