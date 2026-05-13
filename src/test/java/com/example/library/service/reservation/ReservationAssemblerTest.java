package com.example.library.service.reservation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.entity.Reservation;
import com.example.library.entity.Seat;
import com.example.library.entity.StudyRoom;
import com.example.library.mapper.SeatMapper;
import com.example.library.mapper.StudyRoomMapper;
import com.example.library.vo.ReservationVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationAssemblerTest {

    @Mock
    private SeatMapper seatMapper;

    @Mock
    private StudyRoomMapper studyRoomMapper;

    @InjectMocks
    private ReservationAssembler reservationAssembler;

    @Test
    void toPageVo_skipsNullSeatIds_andKeepsReservationRecord() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUserId(3L);
        reservation.setSeatId(null);
        reservation.setStartTime(LocalDateTime.now().plusDays(1));
        reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));

        Page<Reservation> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(reservation));

        Page<ReservationVO> result = reservationAssembler.toPageVO(page);

        assertEquals(1, result.getRecords().size());
        ReservationVO vo = result.getRecords().get(0);
        assertEquals(1L, vo.getId());
        assertNull(vo.getSeatNumber());
        assertNull(vo.getStudyRoomName());
        verify(seatMapper, never()).selectBatchIds(anyCollection());
        verify(studyRoomMapper, never()).selectBatchIds(anyCollection());
    }

    @Test
    void toPageVo_skipsNullRoomIds_andBuildsVo() {
        Reservation reservation = new Reservation();
        reservation.setId(2L);
        reservation.setSeatId(9L);

        Seat seat = new Seat();
        seat.setId(9L);
        seat.setSeatNumber("A-09");
        seat.setStudyRoomId(null);

        when(seatMapper.selectBatchIds(anyCollection())).thenReturn(List.of(seat));

        Page<Reservation> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(reservation));

        Page<ReservationVO> result = reservationAssembler.toPageVO(page);

        assertEquals(1, result.getRecords().size());
        ReservationVO vo = result.getRecords().get(0);
        assertEquals("A-09", vo.getSeatNumber());
        assertNull(vo.getStudyRoomName());
        verify(studyRoomMapper, never()).selectBatchIds(anyCollection());
    }

    @Test
    void toPageVo_buildsSeatAndRoomInfo_whenAssociationsExist() {
        Reservation reservation = new Reservation();
        reservation.setId(3L);
        reservation.setSeatId(11L);

        Seat seat = new Seat();
        seat.setId(11L);
        seat.setSeatNumber("B-01");
        seat.setStudyRoomId(5L);

        StudyRoom room = new StudyRoom();
        room.setId(5L);
        room.setName("一号自习室");

        when(seatMapper.selectBatchIds(anyCollection())).thenReturn(List.of(seat));
        when(studyRoomMapper.selectBatchIds(anyCollection())).thenReturn(List.of(room));

        Page<Reservation> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(reservation));

        Page<ReservationVO> result = reservationAssembler.toPageVO(page);

        ReservationVO vo = result.getRecords().get(0);
        assertEquals("B-01", vo.getSeatNumber());
        assertEquals("一号自习室", vo.getStudyRoomName());
    }
}
