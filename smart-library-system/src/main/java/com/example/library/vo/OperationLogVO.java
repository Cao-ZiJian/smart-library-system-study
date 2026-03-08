package com.example.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志对外展示对象
 */
@Data
public class OperationLogVO {

    private Long id;

    /**
     * 操作人ID
     */
    private Long userId;

    /**
     * 操作人用户名
     */
    private String username;

    /**
     * 操作名称
     */
    private String operation;

    /**
     * 方法签名
     */
    private String method;

    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 结果：SUCCESS/FAIL
     */
    private String result;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
