package com.evallab.agent.fault;

import com.evallab.agent.guard.PromptGuard;
import com.evallab.agent.guard.RefusalTrace;
import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.TraceReport;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** 平台拉取入口，契约见 docs/TRACE_CONTRACT.md。 */
@RestController
public class AgentController {

    private final SimulatedWorkflow workflow;
    private final AgentProperties properties;
    private final AgentDegradation degradation;
    private final PromptGuard promptGuard;

    public AgentController(SimulatedWorkflow workflow, AgentProperties properties,
                           AgentDegradation degradation, PromptGuard promptGuard) {
        this.workflow = workflow;
        this.properties = properties;
        this.degradation = degradation;
        this.promptGuard = promptGuard;
    }

    @PostMapping("/execute")
    public TraceReport execute(@RequestBody ExecuteRequest request) {
        Instant started = Instant.now();
        // 输入守卫先于一切：被拒绝的请求不进工作流，也不受退化开关影响
        String refusal = promptGuard.inspect(request.input());
        if (refusal != null) {
            return RefusalTrace.of(properties.name(), refusal, started);
        }
        boolean degraded = degradation.applies(request);
        // 退化耗时也记进首个 span，保持 trace 内部时间轴与墙钟一致
        long extraMs = degraded ? degradation.delayMs() : 0;
        TraceReport report = workflow.run(request, properties.timeoutDelay(), extraMs);
        return degraded ? degradation.apply(report) : report;
    }
}
