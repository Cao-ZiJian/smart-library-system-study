package com.example.library.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果封装
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final int SUCCESS_CODE = 0;
    public static final int FAILURE_CODE = 1;

    /**
     * 业务状态码，约定：0 表示成功，其他为失败
     */
    private int code;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功，不带数据
     */
    public static <T> Result<T> success() {
        return new Result<>(SUCCESS_CODE, "success", null);
    }

    /**
     * 成功，带数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "success", data);
    }

    /**
     * 失败，带提示信息
     */
    public static <T> Result<T> failure(String message) {
        return new Result<>(FAILURE_CODE, message, null);
    }

    /**
     * 失败，自定义状态码和提示信息
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }
}

