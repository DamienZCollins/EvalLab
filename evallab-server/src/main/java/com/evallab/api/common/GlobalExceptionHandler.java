package com.evallab.api.common;

import com.evallab.core.eval.EvalTaskNotFoundException;
import com.evallab.core.testcase.InvalidTestCaseException;
import com.evallab.core.testcase.TestCaseNotFoundException;
import com.evallab.core.trace.InvalidTraceException;
import com.evallab.core.trace.TraceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** 统一异常处理：领域异常翻译为错误码，未知异常兜底为 500。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Result<Void>> handleApi(ApiException e) {
        return respond(e.errorCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidTraceException.class)
    public ResponseEntity<Result<Void>> handleInvalidTrace(InvalidTraceException e) {
        return ResponseEntity.status(ErrorCode.INVALID_TRACE.status())
                .body(Result.fail(ErrorCode.INVALID_TRACE.code(), e.getMessage()));
    }

    @ExceptionHandler({TraceNotFoundException.class, TestCaseNotFoundException.class,
            EvalTaskNotFoundException.class})
    public ResponseEntity<Result<Void>> handleNotFound(RuntimeException e) {
        return respond(ErrorCode.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidTestCaseException.class)
    public ResponseEntity<Result<Void>> handleInvalidTestCase(InvalidTestCaseException e) {
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.status())
                .body(Result.fail(ErrorCode.INVALID_ARGUMENT.code(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return respond(ErrorCode.INVALID_ARGUMENT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .toList();
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.status())
                .body(Result.fail(ErrorCode.INVALID_ARGUMENT.code(), String.join("; ", details)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        log.debug("请求体解析失败", e);
        return respond(ErrorCode.MALFORMED_BODY, ErrorCode.MALFORMED_BODY.defaultMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND, "接口不存在: /" + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception e) {
        log.error("未处理的异常", e);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }

    private static ResponseEntity<Result<Void>> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.status()).body(Result.fail(code.code(), message));
    }
}
