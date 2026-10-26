package com.evallab.agent.fault;

import com.evallab.contract.trace.EventType;
import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.FaultInjection;
import com.evallab.contract.trace.FaultType;
import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.SpanEvent;
import com.evallab.contract.trace.SpanType;
import com.evallab.contract.trace.TraceReport;
import com.evallab.contract.trace.TraceStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 一个确定性的两步工具链：搜索 → 提取 → 总结。
 *
 * <p>每种故障只改动一处结构，保证归因规则可以据此稳定区分（见 docs/TRACE_CONTRACT.md）。
 */
@Component
public class SimulatedWorkflow {

    private static final String SEARCH_TOOL = "web_search";
    private static final String EXTRACT_TOOL = "extract";

    private final AgentProperties properties;

    public SimulatedWorkflow(AgentProperties properties) {
        this.properties = properties;
    }

    /**
     * @param timeoutDelay TIMEOUT 故障下的阻塞时长，自检上报时传 {@link Duration#ZERO}
     */
    public TraceReport run(ExecuteRequest request, Duration timeoutDelay) {
        FaultInjection fault = request.faultInjection();
        FaultType type = fault == null || fault.type() == null ? null : fault.type();
        if (type == FaultType.TIMEOUT) {
            sleep(timeoutDelay);
        }

        String traceId = UUID.randomUUID().toString();
        String planId = UUID.randomUUID().toString();
        Instant cursor = Instant.now();
        List<SpanDto> spans = new ArrayList<>();

        boolean misunderstand = type == FaultType.INPUT_MISUNDERSTAND;
        String searchTool = misunderstand ? "weather_query" : SEARCH_TOOL;

        cursor = addSpan(spans, planId, null, SpanType.LLM_CALL, null,
                request.input(),
                misunderstand ? "计划：查询天气" : "计划：搜索 → 提取 → 总结",
                null, cursor, 120);

        // 逐层嵌套：规划 → 搜索 → 提取 → 总结，验证平台对父子关系的解析
        SpanCall search = addToolSpan(spans, planId, searchTool, cursor);
        cursor = search.end();

        SpanCall extract = null;
        String summarizeParent = search.spanId();
        if (type != FaultType.STEP_MISSING) {
            extract = addToolSpan(spans, search.spanId(), EXTRACT_TOOL, cursor);
            cursor = extract.end();
            summarizeParent = extract.spanId();
        }

        cursor = addToolSpan(spans, summarizeParent, "summarize", cursor).end();

        if (type == FaultType.TOOL_ERROR) {
            int target = resolveTarget(fault, 0);
            SpanCall broken = target == 1 && extract != null ? extract : search;
            replaceWithError(spans, broken, "工具调用失败: HTTP 500 Internal Server Error");
        }

        // 曲解指令、输出格式错误时 Agent 自身「执行成功」，错误体现在 output 与工具序列上，
        // 由评分器判定为 FAIL，而不是靠 status 传递
        TraceStatus status = type == null ? TraceStatus.SUCCESS : switch (type) {
            case TIMEOUT -> TraceStatus.TIMEOUT;
            case TOOL_ERROR, STEP_MISSING -> TraceStatus.FAILED;
            default -> TraceStatus.SUCCESS;
        };

        String output = renderOutput(type, request.input(), misunderstand);
        long durationMs = Duration.between(spans.get(0).startTime(), cursor).toMillis();
        int tokens = spans.stream().mapToInt(s -> s.tokens() == null ? 0 : s.tokens()).sum();
        return new TraceReport(traceId, properties.name(), status, output, tokens, durationMs, spans);
    }

    private String renderOutput(FaultType type, String input, boolean misunderstand) {
        if (type == FaultType.OUTPUT_FORMAT_ERROR) {
            // 故意截断，JSON Schema 校验会失败
            return "{\"answer\": \"" + input + "\", \"steps\": ";
        }
        if (type == FaultType.TOOL_ERROR) {
            return "执行失败：工具调用返回 HTTP 500";
        }
        if (misunderstand) {
            return "今天多云转晴，气温 18~26 度。";
        }
        if (type == FaultType.STEP_MISSING) {
            return "{\"answer\": \"（缺少提取步骤，结果不完整）\", \"steps\": 2}";
        }
        return "{\"answer\": \"" + input + "\", \"steps\": 3}";
    }

    private int resolveTarget(FaultInjection fault, int fallback) {
        if (fault == null || fault.targetSpan() == null || fault.targetSpan().isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(fault.targetSpan());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Instant addSpan(List<SpanDto> spans, String spanId, String parentId, SpanType type,
                            String toolName, String input, String output, String error,
                            Instant start, long durationMs) {
        Instant end = start.plusMillis(durationMs);
        spans.add(new SpanDto(spanId, parentId, type, toolName, input, output, error,
                estimateTokens(input, output), durationMs, start, end,
                List.of(new SpanEvent(EventType.LOG, toolName == null ? "规划完成" : toolName + " 调用完成", end))));
        return end;
    }

    private SpanCall addToolSpan(List<SpanDto> spans, String parentId, String toolName, Instant start) {
        String spanId = UUID.randomUUID().toString();
        Instant end = addSpan(spans, spanId, parentId, SpanType.TOOL_CALL, toolName,
                "{\"query\": \"...\"}", "{\"result\": \"" + toolName + " 结果\"}",
                null, start, 80);
        return new SpanCall(spanId, toolName, end);
    }

    private void replaceWithError(List<SpanDto> spans, SpanCall target, String message) {
        for (int i = 0; i < spans.size(); i++) {
            SpanDto span = spans.get(i);
            if (!span.spanId().equals(target.spanId())) {
                continue;
            }
            spans.set(i, new SpanDto(span.spanId(), span.parentSpanId(), span.spanType(), span.toolName(),
                    span.input(), null, message, span.tokens(), span.durationMs(),
                    span.startTime(), span.endTime(),
                    List.of(new SpanEvent(EventType.ERROR, message, span.endTime()))));
        }
    }

    private static int estimateTokens(String input, String output) {
        int chars = (input == null ? 0 : input.length()) + (output == null ? 0 : output.length());
        return Math.max(1, chars / 4);
    }

    private static void sleep(Duration delay) {
        if (delay == null || delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record SpanCall(String spanId, String toolName, Instant end) {
    }
}
