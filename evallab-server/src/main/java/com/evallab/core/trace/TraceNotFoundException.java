package com.evallab.core.trace;

/** 指定 trace_id 不存在。 */
public class TraceNotFoundException extends RuntimeException {

    public TraceNotFoundException(String traceId) {
        super("Trace 不存在: " + traceId);
    }
}
