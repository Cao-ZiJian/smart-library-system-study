package com.example.library.vo.dashboard;

import lombok.Data;

import java.util.List;

@Data
public class DashboardOverviewVO {

    private long bookTotal;
    private long bookOnShelfCount;
    private long bookBorrowableCount;
    private long userTotal;

    private long todayBorrowApplyCount;
    private long todayLendCount;
    private long todayReturnCount;
    private long todayReservationCount;

    private long currentOverdueCount;
    private long seatInUseCount;

    private List<HotBookBriefVO> hotBooksTop5;
    private List<StudyRoomUtilizationVO> studyRoomUtilizationTop5;
}
