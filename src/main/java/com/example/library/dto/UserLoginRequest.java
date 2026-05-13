package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户登录请求参数
 */
@Data
public class UserLoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 100, message = "密码长度不能超过100")
    private String password;
}

