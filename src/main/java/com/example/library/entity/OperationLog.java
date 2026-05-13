package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体，对应表 operation_log
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
