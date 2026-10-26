package com.evallab.api.trace;

import com.evallab.api.common.Result;
import com.evallab.contract.trace.TraceReport;
import com.evallab.core.trace.TraceDetail;
import com.evallab.core.trace.TraceIngestResult;
import com.evallab.core.trace.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Trace", description = "Trace 采集与查询")
@RestController
@RequestMapping("/api/trace")
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @Operation(summary = "上报 Trace（补充路径，trace_id 幂等）",
            description = "已存在的 trace_id 不覆盖、不合并，直接返回原 ID。上报的 Trace 不参与评测。")
    @PostMapping
    public Result<TraceIngestResult> report(@RequestBody TraceReport report) {
        return Result.ok(traceService.report(report));
    }

    @Operation(summary = "查询 Trace 详情", description = "Span 按开始时间升序展平返回")
    @GetMapping("/{traceId}")
    public Result<TraceDetail> detail(@PathVariable String traceId) {
        return Result.ok(traceService.get(traceId));
    }
}
