package com.evallab.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

/** 统一返回体。 */
@Schema(description = "统一返回体")
public record Result<T>(
        @Schema(description = "0 表示成功，非 0 为错误码") int code,
        @Schema(description = "错误说明，成功时为 ok") String message,
        @Schema(description = "业务数据") T data
) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
