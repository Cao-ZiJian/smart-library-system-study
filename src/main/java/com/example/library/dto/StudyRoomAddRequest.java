package com.example.library.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 自习室新增请求参数
 */
@Data
public class StudyRoomAddRequest {

    /**
     * 自习室名称
     */
    @NotBlank(message = "自习室名称不能为空")
    @Size(max = 100, message = "自习室名称长度不能超过100")
    private String name;

    /**
     * 位置描述
     */
    @Size(max = 200, message = "位置描述长度不能超过200")
    private String location;

    /**
     * 容量（座位数）
     */
    @NotNull(message = "容量不能为空")
    @Min(value = 0, message = "容量不能为负数")
    private Integer capacity;

    /**
     * 开放时间描述，如 08:00-22:00
     */
    @Size(max = 50, message = "开放时间长度不能超过50")
    private String openTime;

    /**
     * 展示图 URL（可先上传再填入）
     */
    @Size(max = 1024, message = "图片地址过长")
    private String imageUrl;

    /**
     * 状态：1启用 0禁用
     */
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}
