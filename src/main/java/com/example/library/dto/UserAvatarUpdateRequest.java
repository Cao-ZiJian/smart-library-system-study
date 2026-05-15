package com.example.library.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class UserAvatarUpdateRequest {

    @NotBlank(message = "头像地址不能为空")
    @Size(max = 2048, message = "头像地址过长")
    private String avatarUrl;
}
