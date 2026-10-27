package com.evallab.api.common;

import java.util.List;

/** 带错误码的接口异常，由统一异常处理转换为 Result。 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> details;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public ApiException(ErrorCode errorCode, String message, List<String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = List.copyOf(details);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<String> details() {
        return details;
    }
}
