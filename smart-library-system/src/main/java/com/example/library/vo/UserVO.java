package com.example.library.vo;

import lombok.Data;

/**
 * 用户对外展示信息
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String email;

    /**
     * 角色：USER/LIBRARIAN/ADMIN
     */
    private String role;
}

