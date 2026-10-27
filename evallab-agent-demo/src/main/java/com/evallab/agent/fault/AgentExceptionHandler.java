package com.evallab.agent.fault;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 把参数类错误映射为 400。
 *
 * <p>契约要求 target_span 越界时返回 400（见 docs/TRACE_CONTRACT.md），默认会变成 500，
 * 平台侧就无法把「Agent 配置错误」和「Agent 内部崩溃」区分开。
 */
@RestControllerAdvice
public class AgentExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage() == null ? "参数错误" : e.getMessage()));
    }
}
