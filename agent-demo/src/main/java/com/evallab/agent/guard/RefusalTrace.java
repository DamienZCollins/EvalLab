package com.evallab.agent.guard;

import com.evallab.contract.trace.EventType;
import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.SpanEvent;
import com.evallab.contract.trace.SpanType;
import com.evallab.contract.trace.TraceReport;
import com.evallab.contract.trace.TraceStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 输入被拒绝时返回的 Trace。
 *
 * <p>状态取 SUCCESS 而非 FAILED：Agent 正常完成了「拒绝」这一动作，平台的红队守卫正则
 * 会判定为防御成功。若这里返回 FAILED，平台会先按「Agent 状态非 SUCCESS」记失败，
 * 红队报告就会把一次成功的防御算成被攻破。
 *
 * <p>输出里只写拒绝理由，绝不回显攻击载荷——回显本身就是泄露。
 */
public final class RefusalTrace {

    private static final String OUTPUT =
            "{\"answer\": \"无法执行该请求：检测到提示注入特征，已拒绝处理。\", \"blocked\": true}";

    private RefusalTrace() {
    }

    public static TraceReport of(String agentName, String reason, Instant started) {
        Instant end = Instant.now();
        SpanDto span = new SpanDto(UUID.randomUUID().toString(), null, SpanType.LLM_CALL, null,
                null, "已拒绝执行", reason, 1, Math.max(1, end.toEpochMilli() - started.toEpochMilli()),
                started, end,
                List.of(new SpanEvent(EventType.WARNING, reason, end)));
        return new TraceReport(UUID.randomUUID().toString(), agentName, TraceStatus.SUCCESS, OUTPUT, 1,
                span.durationMs(), List.of(span));
    }
}
