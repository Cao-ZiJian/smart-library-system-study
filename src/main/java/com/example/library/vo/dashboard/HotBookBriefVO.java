package com.example.library.vo.dashboard;

import lombok.Data;

@Data
public class HotBookBriefVO {

    private Long bookId;
    private String title;
    private Integer borrowCount;
}
