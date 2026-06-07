package com.example.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户展示信息。
 */
@Data
public class AdminUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String email;

    private String role;

    private Integer status;

    private String avatarUrl;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
