package com.example.library.mapper;

import com.example.library.vo.dashboard.HotBookBriefVO;
import com.example.library.vo.dashboard.StudyRoomUtilizationVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardMapper {

    long countBookTotal();

    long countBookOnShelf();

    long countBookBorrowable();

    long countUserTotal();

    long countTodayBorrowApply();

    long countTodayLend();

    long countTodayReturn();

    long countTodayReservation();

    long countCurrentOverdue();

    long countSeatInUse();

    List<HotBookBriefVO> selectHotBooks(@Param("limit") int limit);

    List<StudyRoomUtilizationVO> selectStudyRoomUtilizationTop(@Param("limit") int limit);
}
