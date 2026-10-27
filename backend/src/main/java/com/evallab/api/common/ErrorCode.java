package com.evallab.api.common;

import org.springframework.http.HttpStatus;

/** 业务错误码，同时携带对应的 HTTP 状态。 */
public enum ErrorCode {

    INVALID_ARGUMENT(40001, HttpStatus.BAD_REQUEST, "请求参数不合法"),
    INVALID_TRACE(40002, HttpStatus.BAD_REQUEST, "Trace 结构不合法"),
    MALFORMED_BODY(40003, HttpStatus.BAD_REQUEST, "请求体无法解析"),
    NOT_FOUND(40401, HttpStatus.NOT_FOUND, "资源不存在"),
    METHOD_NOT_ALLOWED(40501, HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持"),
    INTERNAL_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "服务内部错误");

    private final int code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public int code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
