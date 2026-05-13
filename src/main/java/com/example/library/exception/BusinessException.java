package com.example.library.exception;

/**
 * 业务异常
 * <p>
 * 用于显式抛出可预期的业务错误，交由全局异常处理器转换为统一返回结果。
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;

    /**
     * 业务错误码，默认 400
     */
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = BAD_REQUEST;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
