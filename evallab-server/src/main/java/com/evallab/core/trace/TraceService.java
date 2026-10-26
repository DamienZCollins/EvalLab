package com.evallab.core.trace;

import com.evallab.contract.trace.TraceReport;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Trace 采集与查询。
 *
 * <p>上报路径幂等：trace_id 已存在时直接返回，不覆盖、不合并（见 docs/设计决策.md D1）。
 */
@Service
public class TraceService {

    private final TraceStore store;

    public TraceService(TraceStore store) {
        this.store = store;
    }

    public TraceIngestResult report(TraceReport report) {
        List<String> violations = TraceValidator.validate(report);
        if (!violations.isEmpty()) {
            throw new InvalidTraceException(violations);
        }
        if (store.existsByTraceId(report.traceId())) {
            return new TraceIngestResult(report.traceId(), true);
        }
        try {
            store.save(report, TraceSource.REPORT);
        } catch (DuplicateTraceException e) {
            // 并发上报同一 trace_id：先到者为准
            return new TraceIngestResult(report.traceId(), true);
        }
        return new TraceIngestResult(report.traceId(), false);
    }

    public TraceDetail get(String traceId) {
        return store.findDetail(traceId).orElseThrow(() -> new TraceNotFoundException(traceId));
    }
}
