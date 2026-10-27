package com.evallab.agent.fault;

import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.TraceReport;
import com.evallab.contract.trace.TraceStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运行期退化开关：用于验证回归门禁确实能拦住变差的 Agent。
 *
 * <p>只作用于**未注入故障**的正常用例——故障用例本来就是失败的，再退化也没有观测意义；
 * 未指定 case_ids 时对所有正常用例生效。开关是内存态，Agent 重启即恢复。
 *
 * <ul>
 *   <li>{@code FAIL_STATUS}：输出与工具序列不变，只把状态改成 FAILED，用来单独压低通过率；</li>
 *   <li>{@code BAD_OUTPUT}：返回截断的输出，规则评分失败，用来压低平均分；</li>
 *   <li>{@code SLOW}：额外阻塞 delay_ms，用来抬高 P95 耗时。</li>
 * </ul>
 */
@Component
public class AgentDegradation {

    public enum Mode {
        NONE, FAIL_STATUS, BAD_OUTPUT, SLOW
    }

    /** @param caseIds 为空表示全部正常用例 */
    public record State(Mode mode, long delayMs, Set<String> caseIds) {

        public boolean enabled() {
            return mode != Mode.NONE;
        }
    }

    private static final State IDLE = new State(Mode.NONE, 0, Set.of());

    private volatile State state = IDLE;

    public State current() {
        return state;
    }

    public void configure(Mode mode, Long delayMs, List<String> caseIds) {
        Mode resolved = mode == null ? Mode.NONE : mode;
        long delay = delayMs == null ? 0 : delayMs;
        if (delay < 0) {
            throw new IllegalArgumentException("delay_ms 不能为负数: " + delay);
        }
        if (resolved == Mode.SLOW && delay == 0) {
            throw new IllegalArgumentException("SLOW 模式需要 delay_ms > 0");
        }
        state = new State(resolved, delay,
                caseIds == null ? Set.of() : caseIds.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    public void reset() {
        state = IDLE;
    }

    /** 该请求是否应用退化。 */
    public boolean applies(ExecuteRequest request) {
        State s = state;
        if (!s.enabled() || request.faultInjection() != null) {
            return false;
        }
        // 不可变集合的 contains(null) 会抛 NPE，而自检上报不带 case_id
        return s.caseIds().isEmpty() || (request.caseId() != null && s.caseIds().contains(request.caseId()));
    }

    /** SLOW 模式的附加阻塞时长。 */
    public long delayMs() {
        State s = state;
        return s.mode() == Mode.SLOW ? s.delayMs() : 0;
    }

    /** 按当前模式改写 Trace；SLOW 靠耗时代替，不改内容。 */
    public TraceReport apply(TraceReport report) {
        return switch (state.mode()) {
            case FAIL_STATUS -> new TraceReport(report.traceId(), report.agentName(), TraceStatus.FAILED,
                    report.output(), report.totalTokens(), report.durationMs(), report.spans());
            case BAD_OUTPUT -> new TraceReport(report.traceId(), report.agentName(), report.status(),
                    report.output() == null ? "" : report.output().substring(0,
                            Math.min(8, report.output().length())),
                    report.totalTokens(), report.durationMs(), report.spans());
            case NONE, SLOW -> report;
        };
    }
}
