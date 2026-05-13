package com.example.library.service.impl;

import com.example.library.mapper.DashboardMapper;
import com.example.library.service.DashboardService;
import com.example.library.vo.dashboard.DashboardOverviewVO;
import com.example.library.vo.dashboard.HotBookBriefVO;
import com.example.library.vo.dashboard.StudyRoomUtilizationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;

    @Override
    public DashboardOverviewVO overview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setBookTotal(dashboardMapper.countBookTotal());
        vo.setBookOnShelfCount(dashboardMapper.countBookOnShelf());
        vo.setBookBorrowableCount(dashboardMapper.countBookBorrowable());
        vo.setUserTotal(dashboardMapper.countUserTotal());
        vo.setTodayBorrowApplyCount(dashboardMapper.countTodayBorrowApply());
        vo.setTodayLendCount(dashboardMapper.countTodayLend());
        vo.setTodayReturnCount(dashboardMapper.countTodayReturn());
        vo.setTodayReservationCount(dashboardMapper.countTodayReservation());
        vo.setCurrentOverdueCount(dashboardMapper.countCurrentOverdue());
        vo.setSeatInUseCount(dashboardMapper.countSeatInUse());

        List<HotBookBriefVO> hot = dashboardMapper.selectHotBooks(5);
        vo.setHotBooksTop5(hot == null ? List.of() : hot);

        List<StudyRoomUtilizationVO> util = dashboardMapper.selectStudyRoomUtilizationTop(5);
        vo.setStudyRoomUtilizationTop5(util == null ? List.of() : util);
        return vo;
    }
}
