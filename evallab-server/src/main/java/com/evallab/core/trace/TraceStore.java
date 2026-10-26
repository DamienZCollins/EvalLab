package com.evallab.core.trace;

import com.evallab.contract.trace.TraceReport;

import java.util.Optional;

/** Trace 持久化端口，实现在 infra 层。 */
public interface TraceStore {

    boolean existsByTraceId(String traceId);

    /**
     * 保存 Trace 及其全部 Span。
     *
     * @throws DuplicateTraceException trace_id 已被并发写入
     */
    void save(TraceReport report, TraceSource source);

    Optional<TraceDetail> findDetail(String traceId);
}
