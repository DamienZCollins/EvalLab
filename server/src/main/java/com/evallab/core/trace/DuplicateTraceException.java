package com.evallab.core.trace;

/** trace_id 并发写入冲突，由存储层翻译唯一索引异常得到。 */
public class DuplicateTraceException extends RuntimeException {

    private final String traceId;

    public DuplicateTraceException(String traceId, Throwable cause) {
        super("trace_id 已存在: " + traceId, cause);
        this.traceId = traceId;
    }

    public String traceId() {
        return traceId;
    }
}
