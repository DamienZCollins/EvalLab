package com.evallab.core.trace;

/**
 * 上报结果。
 *
 * @param duplicated true 表示 trace_id 已存在，本次未写入
 */
public record TraceIngestResult(String traceId, boolean duplicated) {
}
