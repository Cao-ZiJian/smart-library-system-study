package com.example.library.exception;

/**
 * 业务异常
 * <p>
 * 用于显式抛出可预期的业务错误，交由全局异常处理器转换为统一返回结果。
 */
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码，默认 400
     */
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

